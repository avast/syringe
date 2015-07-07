/*
 * Copyright (c) 2015 Avast a.s., www.avast.com
 */
package com.avast.syringe.config.perspective.resolver;

import javax.inject.Named;

/**
 * This implementation introspects class' annotation
 * Named and uses its value as a bean name or the class
 * name if the annotation is empty.
 *
 * @author Jan Lastovicka (lastovicka@avast.com)
 * @since 2015-07-01
 */
final class NamedAnnotationAwareNameResolver implements BeanNameResolver {

    @Override
    public boolean canResolve(Class<?> beanType) {
        return isAnnotationPresent(beanType);
    }

    @Override
    public String resolve(Class<?> beanType) {
        if (isAnnotationPresent(beanType)) {
            return annotationValue(beanType);
        } else {
            return simpleName(beanType);
        }
    }

    private boolean isAnnotationPresent(Class<?> beanType) {
        return beanType.isAnnotationPresent(Named.class);
    }

    private String annotationValue(Class<?> beanType) {
        Named annotation = beanType.getAnnotation(Named.class);
        String value = annotation.value();

        if (value.isEmpty()) {
            return simpleName(beanType);
        }
        return value;
    }

    private String simpleName(Class<?> beanType) {
        return beanType.getSimpleName();
    }
}
