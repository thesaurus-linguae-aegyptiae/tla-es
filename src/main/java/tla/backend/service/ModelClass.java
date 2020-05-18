package tla.backend.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import tla.backend.es.model.meta.Indexable;
import tla.backend.es.repo.RepoPopulator;

/**
 * Put this on top of services. It is being used for index population so put it on {@link RepoPopulator} as well.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ModelClass {

    public Class<? extends Indexable> value();

    /**
     * Optional path value assigned to a model class, e.g. to be used for locating an instance in a file system
     */
    public String path() default "";

}