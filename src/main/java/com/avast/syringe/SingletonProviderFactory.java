package com.avast.syringe;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Set;

/**
 * User: slajchrt
 * Date: 6/22/12
 * Time: 11:10 AM
 */
public class SingletonProviderFactory {

    public static <T> Provider<T> createSingletonProvider(Provider<T> provider) {
        Preconditions.checkNotNull(provider, "Provider must not be null");

        Set<Class> interfaces = Sets.newLinkedHashSet();
        Class cls = provider.getClass();
        collectInterfaces(cls, interfaces);

        Class[] intfArray = interfaces.toArray(new Class[interfaces.size()]);

        return (Provider<T>)Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), intfArray,
                new SingletonProviderInvocationHandler(provider));
    }

    public static Object unwrap(Object instance) {
        if (Proxy.isProxyClass(instance.getClass())) {
            InvocationHandler invocationHandler = Proxy.getInvocationHandler(instance);
            if (invocationHandler instanceof SingletonProviderInvocationHandler) {
                return ((SingletonProviderInvocationHandler) invocationHandler).getProvider();
            } else {
                return instance;
            }
        } else {
            return instance;
        }
    }

    private static void collectInterfaces(Class cls, Set<Class> interfaces) {
        interfaces.addAll(Arrays.asList(cls.getInterfaces()));

        cls = cls.getSuperclass();
        if (cls == null || cls != Object.class) {
            collectInterfaces(cls, interfaces);
        }
    }

    public static class SingletonProviderInvocationHandler<T> implements InvocationHandler {

        private final Provider<T> provider;
        private volatile T instance = null;

        private SingletonProviderInvocationHandler(Provider<T> provider) {
            this.provider = provider;
        }

        public T getInstance() {
            return instance;
        }

        public Provider<T> getProvider() {
            return provider;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            if (method.getName().equals("getInstance") && method.getParameterTypes().length == 0) {
                return getInstanceInternal();
            }

            return method.invoke(provider, args);
        }

        private synchronized T getInstanceInternal() throws Exception {
            if (instance == null) {
                instance = provider.getInstance();
            }
            return instance;
        }

    }
}
