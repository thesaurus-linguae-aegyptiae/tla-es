package tla.backend.service;

import tla.backend.es.model.meta.UserFriendlyEntity;
import tla.backend.es.repo.custom.UserFriendlyEntityRepo;
import tla.backend.service.search.AutoCompleteSupport;
import tla.domain.dto.meta.AbstractDto;

public abstract class UserFriendlyEntityService<
    T extends UserFriendlyEntity,
    R extends UserFriendlyEntityRepo<T, String>,
    D extends AbstractDto
> extends EntityService<T, R, D> {

    private AutoCompleteSupport autoComplete = AutoCompleteSupport.builder()
        .queryField("hash", 1F)
        .queryField("hash._2gram", 1F)
        .queryField("hash._3gram", 1F)
        .responseFields(new String[]{"hash"})
        .build();

    @Override
    public T retrieve(String id) {
        return getRepo().findBySUID(id).orElse(
            super.retrieve(id)
        );
    }

    @Override
    public AutoCompleteSupport getAutoCompleteSupport() {
        return this.autoComplete;
    }

}
