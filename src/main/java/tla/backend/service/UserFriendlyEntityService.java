package tla.backend.service;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.index.query.MultiMatchQueryBuilder;

import tla.backend.es.model.meta.UserFriendlyEntity;
import tla.backend.es.repo.custom.UserFriendlyEntityRepo;
import tla.domain.dto.meta.AbstractDto;

public abstract class UserFriendlyEntityService<
    T extends UserFriendlyEntity,
    R extends UserFriendlyEntityRepo<T, String>,
    D extends AbstractDto
> extends EntityService<T, R, D> {

    @Override
    public T retrieve(String id) {
        return getRepo().findBySUID(id).orElse(
            super.retrieve(id)
        );
    }

    @Override
    public MultiMatchQueryBuilder entityLookupAutocompleteQuery(String term) {
        return super.entityLookupAutocompleteQuery(term)
            .field("hash")
            .field("hash._2gram")
            .field("hash._3gram");
    }

    @Override
    public List<String> entityLookupAutocompleteResponseFields() {
        List<String> fields = new ArrayList<>();
        fields.addAll(super.entityLookupAutocompleteResponseFields());
        fields.add("hash");
        return fields;
    }

}