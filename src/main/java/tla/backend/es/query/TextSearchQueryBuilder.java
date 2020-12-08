package tla.backend.es.query;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.TextEntity;
import tla.backend.service.ModelClass;

@Slf4j
@Getter
@ModelClass(TextEntity.class)
public class TextSearchQueryBuilder extends PassportIncludingQueryBuilder implements ExpansionQueryBuilder {

    private boolean expansion;

    private String[] rootIds;

    @Override
    public void setExpansion(boolean expansion) {
        log.info("text query: set IDs aggregation");
        ExpansionQueryBuilder.super.setExpansion(expansion);
        this.expansion = expansion;
    }

    @Override
    public void setRootIds(String[] ids) {
        // TODO Auto-generated method stub
    }

}
