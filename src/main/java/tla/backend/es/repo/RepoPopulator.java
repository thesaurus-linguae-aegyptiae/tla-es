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
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.meta.Indexable;
import tla.backend.service.ModelClass;
import tla.backend.service.EntityService;

/**
 * Repository populator batch-indexing documents read from <code>*.tar.gz</code> file.
 * In order to be able to batch-ingest a document type, a {@link EntityService} implementation
 * must exist and be typed for the model class to which the document is to be mapped.
 * This entity service subclass must be annotated with a {@link ModelClass} annotation specifying
 * the model class <i>and</i> the directory within the TAR archive in which files of the
 * document type in question are located. The entity service subclass must also return
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

    /**
     * Batch indexer capable of deserializing JSON strings into instances of the
     * model class it typed for.
     */
    private class RepoBatchIngestor<S extends Indexable> {

        final static int MAX_BATCH_SIZE = 750;

        private List<S> batch;
        private Class<S> modelClass;
        private String path;
        private int count;

        private ObjectMapper jsonMapper = new ObjectMapper();

        public RepoBatchIngestor(Class<S> modelClass) {
            this.modelClass = modelClass;
            this.path = getModelClassServicePath(
                EntityService.getService(modelClass)
            );
            this.batch = new ArrayList<>();
            this.count = 0;
        }

        public void add(S doc) {
            this.batch.add(doc);
            if (this.batch.size() >= MAX_BATCH_SIZE) {
                this.ingest();
            }
        }

        /**
         * Deserialize a domain model entity from a JSON string and either cache it for later, or batch-index it along all
         * other cached entities in case their number exceeds the threshold defined in {@link #MAX_BATCH_SIZE}.
         *
         * @see #ingest()
         */
        public void add(String json) {
            try {
                S doc = jsonMapper.readValue(
                    json,
                    this.modelClass
                );
                this.add(doc);
            } catch (IOException e) {
                log.info(
                    String.format(
                        "ingestor could not instantiate %s class from %s",
                        this.modelClass.getName(),
                        json.subSequence(0, 30)
                    )
                );
            }
        }

        /**
         * Get the {@link EntityService} instance of which the {@link ModelClass} annotation specifies the
         * same model class as this batch indexer is typed for, and uses that service's {@link ElasticsearchRepository}
         * to batch-index all entities currently in the cache.
         */
        @SuppressWarnings("unchecked")
        public void ingest() {
            try {
                ((ElasticsearchRepository<S, String>) EntityService.getService(modelClass).getRepo()).saveAll(this.batch);
                this.count += this.batch.size();
                this.batch.clear();
            } catch (Exception e) {
                log.error(
                    String.format(
                        "%s ingestor could not save %d docs (%s)! Trying again...",
                        this.modelClass.getSimpleName(),
                        this.batch.size(),
                        e.getClass().getSimpleName()
                    )
                );
            }
        }
    }


    /**
     * batch indexer registry
     */
    private Map<String, RepoBatchIngestor<? extends Indexable>> repoIngestors = new HashMap<>();

    /**
     * currently active batch indexer
     */
    private RepoBatchIngestor<? extends Indexable> batchIngestor = null;

    /**
     * Return the <code>path</code> value of a entity service's {@link ModelClass} annotation
     */
    private static String getModelClassServicePath(EntityService<? extends Indexable> service) {
        for (Annotation a : service.getClass().getAnnotations()) {
            if (a instanceof ModelClass) {
                return ((ModelClass) a).path();
            }
        }
        return null;
    }

    /**
     * Registers {@link RepoBatchIngestor} instances for each model class specified via a {@link ModelClass} annotation
     * on the {@link RepoPopulator} class.
     */
    private void registerRepoIngestors() {
        for (Class<? extends Indexable> modelClass : EntityService.getRegisteredModelClasses()) {
            String modelPath = getModelClassServicePath(
                EntityService.getService(modelClass)
            );
            if (modelPath != null && !modelPath.isEmpty()) {
                this.repoIngestors.put(
                    modelPath,
                    new RepoBatchIngestor<>(modelClass)
                );
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
            try (
                TarArchiveInputStream input = new TarArchiveInputStream(
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

    /**
     * Tries to get a document type identifier out of an archived item's path,
     * which is the hindmost path segment for directories, and the second path segment from the end
     * for file entries.
     *
     * @return path segment thought to represent a doctype
     */
    private String extractDocTypeFromPath(TarArchiveEntry archiveEntry) {
        String[] segments = archiveEntry.getName().split("/");
        int segmentOffset = archiveEntry.isDirectory() ? 1 : 2;
        return segments[
            segments.length - segmentOffset
        ];
    }

    /**
     * switches to the appropriate batch indexer for a given path.
     *
     * @return might be null
     */
    private void selectBatchIngestor(String modelPath) {
        if (this.batchIngestor == null || !this.batchIngestor.path.equals(modelPath)) {
            if (this.repoIngestors.containsKey(modelPath)) {
                this.batchIngestor = this.repoIngestors.get(modelPath);
                log.info(
                    "use {} model batch ingestor for directory {}",
                    this.batchIngestor.modelClass.getSimpleName(),
                    modelPath
                );
            } else {
                this.batchIngestor = null;
            }
        }
    }

    /**
     * Go through a TAR archive and uses the appropriate {@link ElasticsearchRepository> to index
     * JSON files.
     */
    private void processTarArchive(TarArchiveInputStream input) throws IOException {
        TarArchiveEntry archiveEntry;
        while ((archiveEntry = input.getNextTarEntry()) != null) {
            String typeId = this.extractDocTypeFromPath(archiveEntry);
            this.selectBatchIngestor(typeId);
            if (!archiveEntry.isDirectory()) {
                if (input.canReadEntryData(archiveEntry)) {
                    this.selectBatchIngestor(typeId);
                    if (this.batchIngestor != null) {
                        this.batchIngestor.add(
                            new String(input.readAllBytes())
                        );
                    }
                } else {
                    log.warn("archived file {} not readable", archiveEntry.getName());
                }
            }
        }
        flushIngestors();
    }

    private void flushIngestors() {
        this.batchIngestor = null;
        for (RepoBatchIngestor<? extends Indexable> batchIngestor : this.repoIngestors.values()) {
            batchIngestor.ingest();
            log.info(
                "ingested {} documents of type {}",
                batchIngestor.count,
                batchIngestor.modelClass.getSimpleName()
            );
        }
    }

}