package com.avast.syringe.aop;

import java.lang.reflect.Method;

/**
 * User: vacata
 * Date: 2/18/13
 * Time: 11:46 AM
 */
public interface MethodInvocation<T> {

    /**
     * Invoke original (unproxied) method on the original (possibly unproxied) target.
     * @return Return value of the original method.
     * @throws Throwable May occur in case of:
     * <ul>
     *     <li>Target method code itself.</li>
     *     <li>Invocation of the original method. These exceptions won't be wrapped into {@link java.lang.reflect.InvocationTargetException}.</li>
     * </ul>
     */
    Object invoke() throws Throwable;

    /**
     * Gets the original (possibly unproxied) instance of {@code T}.
     * @return The original (possibly unproxied) instance of {@code T}.
     */
    T getRawTarget();

    /**
     * Gets the proxied instance of {@code T}.
     * @return The proxied instance of {@code T}.
     */
    T getProxy();

    /**
     * Gets the method whose invocation is being intercepted.
     * @return The method whose invocation is being intercepted.
     */
    Method getMethod();

    /**
     * Gets array of arguments being passed in the original method call.
     * @return Array of arguments being passed in the original method call.
     */
    Object[] getArgs();
}
