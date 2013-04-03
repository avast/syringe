package com.avast.syringe.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a configuration property in a configuration class.
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigProperty {

    @Deprecated
    enum Habitat {
        DEFAULT,
        CONFIG,
        /**
         * No schema element is generated for it, and the value is taken from a context when being injected
         */
        CONTEXT
    }

    boolean optional() default false;

    @Deprecated
    Habitat habitat() default Habitat.DEFAULT;

    /**
     * @return true if the config property represents the delegate reference of a decorator
     */
    boolean delegate() default false;

    /**
     * Arbitrary tags. They can used when resolving 'context' values for properties in resolvers.
     */
    String[] tags() default {};
}
