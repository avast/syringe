/*
 * Copyright (c) 2015 Avast a.s., www.avast.com
 */
package com.avast.syringe.config.internal;

import com.avast.syringe.config.ConfigProperty;
import com.avast.syringe.config.PropertyValueConverter;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.util.Optional;

/**
 * A factory of InjectableProperty from Field based on
 * provided metadata. At this time the metadata is
 * represented by annotations @ConfigProperty and @Named
 *
 * @author Jan Lastovicka (lastovicka@avast.com)
 * @since 2015-07-01
 */
final class InjectablePropertyFactory {

    private final PropertyValueConverter converter;

    InjectablePropertyFactory(PropertyValueConverter converter) {
        this.converter = converter;
    }

    Optional<InjectableProperty> newProperty(Field field) {
        if (isConfigPropertyPresent(field)) {
            return Optional.of(newConfigPropertyAnnotatedProperty(field));
        }
        if (isInjectPresent(field)) {
            return Optional.of(newInjectAnnotatedProperty(field));
        }
        return Optional.empty();
    }

    private boolean isConfigPropertyPresent(Field field) {
        return field.isAnnotationPresent(ConfigProperty.class);
    }

    private boolean isInjectPresent(Field field) {
        return field.isAnnotationPresent(Inject.class);
    }

    private InjectableProperty newConfigPropertyAnnotatedProperty(Field field) {
        ConfigProperty annotation = configProperty(field);

        return  new ReflectionInjectableProperty(
                field,
                annotation.optional(),
                annotation.name(),
                annotation.habitat(),
                annotation.delegate(),
                converter
        );
    }

    private ConfigProperty configProperty(Field field) {
        return field.getAnnotation(ConfigProperty.class);
    }

    private InjectableProperty newInjectAnnotatedProperty(Field field) {
        return  new ReflectionInjectableProperty(
                field,
                false,
                null,
                ConfigProperty.Habitat.DEFAULT,
                false,
                converter
        );
    }
}
