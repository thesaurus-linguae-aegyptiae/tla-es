package tla.backend.es.repo.custom;

import java.util.Optional;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.repository.NoRepositoryBean;

import tla.domain.model.meta.UserFriendly;

@NoRepositoryBean
public interface UserFriendlyEntityRepo<T extends UserFriendly, ID> extends ElasticsearchRepository<T, ID> {

    public Optional<T> findBySUID(String suid);

}