/*
 * Copyright (c) 2015 Avast a.s., www.avast.com
 */
package com.avast.syringe.config.perspective.resolver;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.Collection;

/**
 * This class represents a resolution of a bean name from a class
 * that is used as a name of singleton and new-instance method in
 * a palette.
 *
 * @author Jan Lastovicka (lastovicka@avast.com)
 * @since 2015-07-01
 */
public final class BeanNameResolution {

    private final Collection<BeanNameResolver> resolvers;

    public BeanNameResolution() {
        resolvers = new ArrayList<>();

        resolvers.add(new ConfigBeanAnnotationAwareNameResolver());
        resolvers.add(new NamedAnnotationAwareNameResolver());
        resolvers.add(new SimpleBeanNameResolver());
    }

    public Optional<String> resolve(Class<?> beanType) {
        for(BeanNameResolver resolver : resolvers) {
            if (resolver.canResolve(beanType)) {
                String name = resolver.resolve(beanType);
                return Optional.of(name);
            }
        }
        return Optional.absent();
    }

    public String resolveOrThrowException(Class<?> beanType) {
        Optional<String> maybeResolved = resolve(beanType);

        if (!maybeResolved.isPresent()) {
            throw new IllegalStateException("Bean name has not been resolved for class " + beanType);
        }
        return maybeResolved.get();
    }
}
