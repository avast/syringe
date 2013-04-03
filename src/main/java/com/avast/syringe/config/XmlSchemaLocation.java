package com.avast.syringe.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a configuration class to specify the location of the XSD schema
 * describing the configuration.
 */
@Deprecated
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface XmlSchemaLocation {

    String value();
}
