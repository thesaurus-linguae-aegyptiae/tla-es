package tla.backend.service;

import java.util.SortedMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tla.backend.es.model.LemmaEntity;
import tla.backend.es.repo.LemmaRepo;

@Service
public class LemmaService extends QueryService<LemmaEntity> {

    @Autowired
    private LemmaRepo repo;

    public SortedMap<String, Long> countOccurrencesPerText(String lemmaId) {
        return null;
    }

    @Override
    public LemmaEntity retrieve(String id) {
        return repo.findById(id).get();
    }

}