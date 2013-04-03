package com.avast.syringe.aop;

/**
 * User: vacata
 * Date: 1/29/13
 * Time: 4:25 PM
 */
public interface ProxyFactory {
    boolean canCreate(Class<?>[] types);

    Object createProxy(Interceptor interceptor, Class<?>[] types, MethodPointcut methodPointcut);
}
