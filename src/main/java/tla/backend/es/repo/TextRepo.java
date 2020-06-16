package tla.backend.es.repo;

import tla.backend.es.model.TextEntity;
import tla.backend.es.repo.custom.UserFriendlyEntityRepo;

public interface TextRepo extends UserFriendlyEntityRepo<TextEntity, String> {}