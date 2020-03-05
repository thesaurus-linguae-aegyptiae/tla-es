package tla.backend.service;

import java.util.SortedMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LemmaService extends QueryService {

    public SortedMap<String, Long> countOccurrencesPerText(String lemmaId) {
        return null;
    }

}