package tla.backend.service;

import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tla.backend.es.model.TextEntity;
import tla.backend.es.repo.TextRepo;

@Service
public class TextService {

    @Autowired
    private TextRepo textRepo;

    @Autowired
    private ThesaurusService thsService;


    /** 
     * Returns first and last year of the time span a text has been attributed to. 
     *
    */
    public int[] getTimespan(String textId) {
        TextEntity text = textRepo.findById(textId).get();
        SortedSet<Integer> years = new TreeSet<>();
        thsService.extractThsEntriesFromPassport(
            text.getPassport(),
            "date.date.date"
        ).stream().forEach(
            term -> {
                years.addAll(
                    term.extractTimespan()
                );
            }
        );
        return new int[] {
            years.first(),
            years.last()
        };
    }

}