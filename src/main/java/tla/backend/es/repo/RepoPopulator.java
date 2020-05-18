package tla.backend.es.repo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.meta.Indexable;
import tla.backend.service.ModelClass;
import tla.backend.service.QueryService;

/**
 * Repository populator batch-indexing documents read from <code>*.tar.gz</code> file.
 * In order to be able to batch-ingest a document type, a {@link QueryService} implementation
 * must exist and be typed for the model class to which the document is to be mapped.
 * This query service subclass must be annotated with a {@link ModelClass} annotation specifying
 * the model class <i>and</i> the directory within the TAR archive in which files of the
 * document type in question are located. The query service subclass must also return
 * the correct {@link ElasticsearchRepository} instance necessary to batch-index a bunch of
 * documents using its {@link ElasticsearchRepository#saveAll(Iterable)} method.
 *
 * I.e. the service class should look like this:
 *
 * <pre>
 * @Service
 * @ModelClass(value = LemmaEntity.class, path = "lemma")
 * public class LemmaService extends QueryService<LemmaEntity> {
 *
 *     @Autowired
 *     private LemmaRepo repo;
 *
 * </pre>
 *
 * An instance of this class is being injected into the main application and automatically run
 * if the application is being executed using the <code>--data-file=</code> argument. If you want
 * it to shut down after populating the database, pass the <code>--shutdown</code> flag.
 *
 * <pre>
 * bootRun --args="--date-file=sample.tar.gz --shutdown"
 * </pre>
 *
 */
@Slf4j
public class RepoPopulator {

    private class RepoBatchIngestor<S extends Indexable> {

        final static int MAX_BATCH_SIZE = 750;

        private List<S> batch;
        private Class<S> modelClass;
        private int count;

        private ObjectMapper jsonMapper = new ObjectMapper();

        public RepoBatchIngestor(Class<S> modelClass) {
            this.modelClass = modelClass;
            this.batch = new ArrayList<>();
            this.count = 0;
        }

        public void add(S doc) {
            this.batch.add(doc);
            if (this.batch.size() >= MAX_BATCH_SIZE) {
                this.ingest();
            }
        }

        public void add(String json) {
            try {
                S doc = jsonMapper.readValue(
                    json,
                    this.modelClass
                );
                this.add(doc);
            } catch (IOException e) {
                log.error(
                    String.format(
                        "ingestor could not instantiate %s class from %s",
                        this.modelClass.getName(),
                        json.subSequence(0, 30)
                    ),
                    e
                );
            }
        }

        @SuppressWarnings("unchecked")
        public void ingest() {
            try {
                ((ElasticsearchRepository<S, String>) QueryService.getService(modelClass).getRepo()).saveAll(this.batch);
            } catch (Exception e) {
                log.error(
                    String.format(
                        "%s ingestor could not save %d docs!",
                        this.modelClass.getName(),
                        this.batch.size()
                    ),
                    e
                );
            } finally {
                this.count += this.batch.size();
                this.batch.clear();
            }
        }
    }

    private Map<String, RepoBatchIngestor<? extends Indexable>> repoIngestors = new HashMap<>();

    /**
     * Registers {@link RepoBatchIngestor} instances for each model class specified via a {@link ModelClass} annotation
     * on the {@link RepoPopulator} class.
     */
    private void registerRepoIngestors() {
        for (Class<? extends Indexable> modelClass : QueryService.getRegisteredModelClasses()) {
            QueryService<? extends Indexable> service = QueryService.getService(modelClass);
            for (Annotation a : service.getClass().getAnnotations()) {
                if (a instanceof ModelClass) {
                    String modelPath = ((ModelClass) a).path();
                    if (modelPath != null && !modelPath.isEmpty()) {
                        repoIngestors.put(
                            modelPath,
                            new RepoBatchIngestor<>(modelClass)
                        );
                    }
                }
            }
        }
    }

    /**
     * Indexes all documents inside a <code>*.tar.gz</code> file at the specified location.
     * @param filenames List of length 1
     * @throws IOException
     */
    public void ingestTarFile(List<String> filenames) throws IOException {
        registerRepoIngestors();
        log.info("process tar file {}", String.join(", ", filenames));
        if (filenames.size() == 1) {
            String filename = filenames.get(0);
            try (TarArchiveInputStream input = new TarArchiveInputStream(
                    new GzipCompressorInputStream(
                        new FileInputStream(filename)
                    )
                )
            ) {
                processTarArchive(input);
            } catch (FileNotFoundException e) {
                log.error(
                    String.format("file not found: %s", filename),
                    e
                );
            } catch (IOException e) {
                log.error(
                    String.format("error during processing tar archive %s", filename),
                    e
                );
            }
        }
        repoIngestors.clear();
    }

    private void processTarArchive(TarArchiveInputStream input) throws IOException {
        TarArchiveEntry archiveEntry;
        RepoBatchIngestor<? extends Indexable> batchIngestor = null;
        while ((archiveEntry = input.getNextTarEntry()) != null) {
            if (archiveEntry.isDirectory()) {
                String[] segments = archiveEntry.getName().split("/");
                String typeId = segments[segments.length - 1];
                if (this.repoIngestors.containsKey(typeId)) {
                    log.info("directory {}", archiveEntry.getName());
                    batchIngestor = this.repoIngestors.get(typeId);
                } else {
                    batchIngestor = null;
                }
            } else {
                if (input.canReadEntryData(archiveEntry)) {
                    String json = new String(input.readAllBytes());
                    if (batchIngestor != null) {
                        batchIngestor.add(json);
                    }
                } else {
                    log.warn("archived file {} not readable", archiveEntry.getName());
                }
            }
        }
        flushIngestors();
    }

    private void flushIngestors() {
        for (RepoBatchIngestor<? extends Indexable> batchIngestor : this.repoIngestors.values()) {
            batchIngestor.ingest();
            log.info(
                "ingested {} documents of type {}",
                batchIngestor.count,
                batchIngestor.modelClass.getName()
            );
        }
    }

}