package tla.backend.es.model.meta;

import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tla.domain.model.meta.UserFriendly;

/**
 * Direct subclass of {@link TLAEntity}, augmenting it with the capability of
 * providing an additional unique identifier.
 */
@SuperBuilder
@NoArgsConstructor
public abstract class UserFriendlyEntity extends TLAEntity implements UserFriendly {}