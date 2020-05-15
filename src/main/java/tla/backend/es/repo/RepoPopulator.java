package tla.backend.es.repo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.AnnotationEntity;
import tla.backend.es.model.CorpusObjectEntity;
import tla.backend.es.model.meta.Indexable;
import tla.backend.es.model.LemmaEntity;
import tla.backend.es.model.OccurrenceEntity;
import tla.backend.es.model.TextEntity;
import tla.backend.es.model.ThsEntryEntity;

@Slf4j
public class RepoPopulator {

    private class RepoBatchIngestor<T extends ElasticsearchRepository<S, String>, S extends Indexable> {

        final static int MAX_BATCH_SIZE = 750;

        private List<S> batch;
        private T repo;
        private Class<S> modelClass;
        private int count;

        private ObjectMapper jsonMapper = new ObjectMapper();

        public RepoBatchIngestor(T repo, Class<S> modelClass) {
            this.repo = repo;
            log.info("modelclass: {}, repo: {}", modelClass.getName(), repo);
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

        public void ingest() {
            try {
                this.repo.saveAll(this.batch);
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

    @Autowired
    private LemmaRepo lemmaRepo;

    @Autowired
    private TextRepo textRepo;

    @Autowired
    private ThesaurusRepo thesaurusRepo;

    @Autowired
    private OccurrenceRepo occurrenceRepo;

    @Autowired
    private AnnotationRepo annotationRepo;

    @Autowired
    private CorpusObjectRepo corpusobjectRepo;

    private static Map<String, RepoBatchIngestor<?,?>> INGESTORS;

    public void ingestTarFile(List<String> filenames) throws IOException {
        INGESTORS = Map.of(
            "text", new RepoBatchIngestor<ElasticsearchRepository<TextEntity,String>,TextEntity>(textRepo, TextEntity.class),
            "lemma", new RepoBatchIngestor<ElasticsearchRepository<LemmaEntity,String>,LemmaEntity>(lemmaRepo, LemmaEntity.class),
            "ths", new RepoBatchIngestor<ElasticsearchRepository<ThsEntryEntity,String>,ThsEntryEntity>(thesaurusRepo, ThsEntryEntity.class),
            "occurrence", new RepoBatchIngestor<ElasticsearchRepository<OccurrenceEntity,String>,OccurrenceEntity>(occurrenceRepo, OccurrenceEntity.class),
            "annotation", new RepoBatchIngestor<ElasticsearchRepository<AnnotationEntity,String>,AnnotationEntity>(annotationRepo, AnnotationEntity.class),
            "object", new RepoBatchIngestor<ElasticsearchRepository<CorpusObjectEntity,String>,CorpusObjectEntity>(corpusobjectRepo, CorpusObjectEntity.class)
        );
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
        INGESTORS = null;
    }

    private void processTarArchive(TarArchiveInputStream input) throws IOException {
        TarArchiveEntry archiveEntry;
        RepoBatchIngestor<? extends ElasticsearchRepository<? extends Indexable, String>, ? extends Indexable> batchIngestor = null;
        while ((archiveEntry = input.getNextTarEntry()) != null) {
            if (archiveEntry.isDirectory()) {
                String[] segments = archiveEntry.getName().split("/");
                String typeId = segments[segments.length - 1];
                if (INGESTORS.containsKey(typeId)) {
                    log.info("directory {}", archiveEntry.getName());
                    batchIngestor = INGESTORS.get(typeId);
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
        for (RepoBatchIngestor<? extends ElasticsearchRepository<? extends Indexable, String>, ? extends Indexable> batchIngestor : INGESTORS.values()) {
            batchIngestor.ingest();
            log.info(
                "ingested {} documents of type {}",
                batchIngestor.count,
                batchIngestor.modelClass.getName()
            );
        }
    }

}