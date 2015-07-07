/*
 * Copyright (c) 2015 Avast a.s., www.avast.com
 */
package com.avast.syringe.config.perspective.resolver;

/**
 * This implementation resoles a bean name as
 * class name.
 *
 * @author Jan Lastovicka (lastovicka@avast.com)
 * @since 2015-07-01
 */
public class SimpleBeanNameResolver implements BeanNameResolver {

    @Override
    public boolean canResolve(Class<?> beanType) {
        return true;
    }

    @Override
    public String resolve(Class<?> beanType) {
        return beanType.getSimpleName();
    }
}
