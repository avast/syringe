package com.avast.syringe.aop;

/**
 * User: vacata
 * Date: 2/18/13
 * Time: 5:14 PM
 *
 * Simple proxy factory returning {@code NULL} while proxying for every possible combination of input arguments.
 */
public class NoOpProxyFactory implements ProxyFactory {
    @Override
    public boolean canCreate(Class<?>[] types) {
        return true;
    }

    @Override
    public Object createProxy(Interceptor interceptor, Class<?>[] types, MethodPointcut methodPointcut) {
        //Returns null proxy for every possible input
        return null;
    }
}
