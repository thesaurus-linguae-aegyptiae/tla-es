package tla.backend.es.repo;

import tla.backend.es.model.ThsEntryEntity;
import tla.backend.es.repo.custom.UserFriendlyEntityRepo;

public interface ThesaurusRepo extends UserFriendlyEntityRepo<ThsEntryEntity, String> {}