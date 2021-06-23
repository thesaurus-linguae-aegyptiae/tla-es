/**
 * This package contains query mapping and expansion logic and abstraction.
 *
 * The idea is that incoming {@link tla.domain.command.SearchCommand} objects sent by a client (like the frontend)
 * are automatically mapped to corresponding query builder objects according to mappings defined
 * in {@link tla.backend.es.model.meta.ModelConfig}, so that translation of individual aspects of a search command into
 * Elasticsearch boolean compound query clauses can be handled in field setter methods on an
 * atomic level, without the need to actually call all these setters by hand.
 *
 * Furthermore, there is a kinda clumsy abstraction for query dependency tree execution which to at
 * least some degree is capable of emulating JOIN queries.
 *
 * @see tla.backend.es.query.ESQueryBuilder
 * @see tla.backend.es.query.ESQueryResult
 *
 * @author jhoeper
 */
package tla.backend.es.query;
