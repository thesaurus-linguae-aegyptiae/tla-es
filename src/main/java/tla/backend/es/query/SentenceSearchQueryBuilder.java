package tla.backend.es.query;

import java.util.Collection;

import static org.elasticsearch.index.query.QueryBuilders.*;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.SentenceEntity;
import tla.backend.service.ModelClass;
import tla.domain.command.PassportSpec;

@Slf4j
@Getter
@ModelClass(SentenceEntity.class)
public class SentenceSearchQueryBuilder extends ESQueryBuilder implements MultiLingQueryBuilder {

    public final static String AGG_ID_TEXT_IDS = "text_ids";
    
	@Override
public void setId(String[] ids) {
    if (ids != null) {
    	 BoolQueryBuilder idQuery = boolQuery();
        log.info("add {} IDs to query", ids.length);
        for ( int i = 0; i < ids.length; i++) {
        	idQuery.must(
                    QueryBuilders.nestedQuery(
                            "tokens",
                            QueryBuilders.termQuery("tokens.id", ids[i]),
                            ScoreMode.None
                        )
                    );

        }
       // idQuery.must().add(QueryBuilders.termQuery("_index", "sentence"));        
    this.filter(idQuery);
}
	}
    

    
/*@Override
public void setId(String[] ids) {
    if (ids != null) {
    	 BoolQueryBuilder idQuery = boolQuery();
        log.info("add {} IDs to query", ids.length);
        for ( int i = 0; i < ids.length; i++) {
        	idQuery.must(QueryBuilders.termQuery("context.textId",ids[i]));

        }
       // idQuery.must().add(QueryBuilders.termQuery("_index", "sentence"));        
    this.filter(idQuery);
}
	}  

*/
    public void setTokens(Collection<TokenSearchQueryBuilder> tokenQueries) {
        BoolQueryBuilder tokenQuery = boolQuery();
        if (tokenQueries != null) {
            tokenQueries.forEach(
                query -> tokenQuery.must(
                    QueryBuilders.nestedQuery(
                        "tokens",
                        query.getNativeRootQueryBuilder(),
                        ScoreMode.None
                    )
                )
            );
        }
        this.filter(tokenQuery);
    }

    public void setPassport(PassportSpec spec) {
        log.info("set sentence search passport specs");
        if (spec != null && !spec.isEmpty()) {
            log.info("spawn text search dependency");
            var textSearchQuery = new TextSearchQueryBuilder();
            textSearchQuery.setExpansion(true);
            textSearchQuery.setPassport(spec);
            this.dependsOn(
                textSearchQuery,
                this::setTextIds
            );
        }
    }

    public void setTextIds(Collection<String> textIds) {
        if (textIds != null) {
            log.info("sentence query: receive {} text IDs", textIds.size());
            this.filter(
                QueryBuilders.termsQuery(
                    "context.textId",
                    textIds
                )
            );
        }
    }

}