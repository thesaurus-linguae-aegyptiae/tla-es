package tla.backend.es.query;

import org.elasticsearch.index.query.QueryBuilders;

import tla.domain.model.SentenceToken.Lemmatization;

public class TokenSearchQueryBuilder extends ESQueryBuilder {

    public void setLemma(Lemmatization lemma) {
        if (lemma != null && !lemma.isEmpty()) {
            this.must(
                QueryBuilders.termQuery("tokens.lemma.id", lemma.getId())
            );
        }
    }

}
