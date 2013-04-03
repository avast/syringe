package com.avast.syringe.config;

import com.avast.syringe.config.perspective.Module;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a configuration bean. It is not necessary to mark every config bean since a single presence of
 * {@link ConfigProperty} makes the class config bean too. It is useful for marking classes having no property.
 * Otherwise the XML and XSD generators do not recognize the class as config bean.
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigBean {
    /**
     * @return bean name
     */
    String value() default "";

    Class<?> extend() default Module.class;
}
