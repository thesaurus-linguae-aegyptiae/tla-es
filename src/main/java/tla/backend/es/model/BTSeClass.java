package tla.backend.es.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation on a subclass of {@link IndexedEntity} in order to specify
 * the value returned by its {@link IndexedEntity#getEclass()} method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BTSeClass {

    public String value();

}