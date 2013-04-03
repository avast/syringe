package com.avast.syringe.aop;

/**
 * User: vacata
 * Date: 1/29/13
 * Time: 11:29 AM
 */
public interface Interceptor<T> {

    /**
     * Core of the interceptor implementation. May add additional code (advice) as well as call the original method.
     * @param invocation Instance of {@link MethodInvocation} containing all data relevant to the original method invocation
     *                   and its arguments, allowing fastest possible invocation of the original method.
     * @return Value to be returned from the intercepted method call.
     * @throws Throwable Check description of {@link MethodInvocation}.
     */
    Object intercept(MethodInvocation<T> invocation) throws Throwable;

    /**
     * Gets the original instance of {@code T} (before applying the interceptor/proxy pattern).
     * @return The original instance of {@code T}. It may not be "pure" (i.e. proxy-free) instance of {@code T} because it may be already
     * proxied with other interceptors.
     */
    T getTarget();
}
