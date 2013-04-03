package com.avast.syringe.aop;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;

/**
 * User: vacata
 * Date: 1/29/13
 * Time: 11:40 AM
 */
public class InterfacePointcut implements MethodPointcut {

    private static Logger logger = LoggerFactory.getLogger(InterfacePointcut.class);

    private final List<Class<?>> interfaces;

    public InterfacePointcut(List<Class<?>> interfaces) {
        this.interfaces = ImmutableList.copyOf(interfaces);
    }

    @Override
    public boolean accept(Method method) {
        for (Class<?> iface : interfaces) {
            try {
                Method foundMethod = iface.getMethod(method.getName(), method.getParameterTypes());
                logger.debug("Accepting method {}.", method);
                return true;
            } catch (NoSuchMethodException e) {
                continue;
            }
        }
        return false;
    }
}
