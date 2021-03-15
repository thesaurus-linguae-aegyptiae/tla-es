package tla.backend.es.query;

import static org.elasticsearch.index.query.QueryBuilders.*;

import org.elasticsearch.index.query.BoolQueryBuilder;

import tla.domain.command.TranslationSpec;
import tla.domain.model.Language;

public interface MultiLingQueryBuilder extends TLAQueryBuilder {

    public default void setTranslation(TranslationSpec translation) {
        BoolQueryBuilder translationsQuery = boolQuery();
        if (translation != null && translation.getLang() != null) {
            var termSpecified = translation.getText() != null && !translation.getText().isBlank();
            for (Language lang : translation.getLang()) {
                translationsQuery.should(
                    termSpecified
                    ? matchQuery(
                        String.format("%stranslations.%s", this.nestedPath(), lang),
                        translation.getText()
                    )
                    : existsQuery(
                        String.format("%stranslations.%s", this.nestedPath(), lang)
                    )
                );
            }
        }
        this.should(translationsQuery);
    }

}
