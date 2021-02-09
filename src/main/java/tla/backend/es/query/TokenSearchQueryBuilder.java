package tla.backend.es.query;

import org.elasticsearch.index.query.QueryBuilders;

import tla.domain.model.SentenceToken.Lemmatization;

public class TokenSearchQueryBuilder extends ESQueryBuilder implements MultiLingQueryBuilder {

    @Override
    public String nestedPath() {
        return "tokens.";
    }

    public void setLemma(Lemmatization lemma) {
        if (lemma != null && !lemma.isEmpty()) {
            this.must(
                QueryBuilders.termQuery(
                    String.format("%slemma.id", this.nestedPath()),
                    lemma.getId()
                )
            );
        }
    }

}
