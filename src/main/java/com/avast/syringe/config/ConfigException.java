package com.avast.syringe.config;

import com.avast.syringe.config.internal.Property;

public class ConfigException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String propertyName;
    private final String beanClassName;

    public ConfigException(Property property, Class beanClass, String message, Throwable cause) {
        this(property.getName(), beanClass.getName(), message, cause);
    }

    public ConfigException(String propertyName, String beanClassName, String message, Throwable cause) {
        super("Injection error for property " + propertyName + " in bean class " + beanClassName + ": " + message,
                cause);
        this.propertyName = propertyName;
        this.beanClassName = beanClassName;
    }

    public ConfigException(String propertyName, String beanClassName, String message) {
        this(propertyName, beanClassName, message, null);
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getBeanClassName() {
        return beanClassName;
    }
}
