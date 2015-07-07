/*
 * Copyright (c) 2015 Avast a.s., www.avast.com
 */
package com.avast.syringe.config.perspective.resolver;

/**
 *
 *
 * @author Jan Lastovicka (lastovicka@avast.com)
 * @since 2015-07-01
 */
public interface BeanNameResolver {

    boolean canResolve(Class<?> beanType);

    String resolve(Class<?> beanType);
}
