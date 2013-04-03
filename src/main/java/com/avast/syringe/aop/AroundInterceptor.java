package com.avast.syringe.aop;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * User: vacata
 * Date: 1/31/13
 * Time: 2:57 PM
 */
public abstract class AroundInterceptor<T> extends AbstractInterceptor<T> {

    private static Logger logger = LoggerFactory.getLogger(AroundInterceptor.class);

    @Override
    public final Object intercept(MethodInvocation<T> invocation) throws Throwable {

        Map<String, Object> context = Maps.newHashMap();
        before(invocation.getProxy(), invocation.getMethod(), invocation.getArgs(), context);

        Object result = null;
        Throwable error = null;
        Throwable errorToThrow = null;
        try {
            result = invocation.invoke();
        } catch (Throwable e) {
            if (e instanceof InterceptionException) {
                throw e;
            }
            logger.debug("Processing exception {} (detail: {}).", e.getClass(), e.getMessage());
            error = e;
        }
        if (error == null) {
            after(invocation.getProxy(), invocation.getMethod(), invocation.getArgs(), context);
            return result;
        } else {
            try {
                errorToThrow = handleException(invocation.getProxy(), invocation.getMethod(), invocation.getArgs(), error, context);
            } catch (Exception e) {
                throw new InterceptionException("Unexpected exception in HANDLE EXCEPTION method!");
            }
            if (errorToThrow == null) {
                throw new InterceptionException("Caught exception shouldn't be swallowed as the returning value is undefined in that case!");
            }
            throw errorToThrow;
        }
    }

    /**
     * Action (advice) to be executed <b>before</b> execution of the original method.
     * @param proxy The proxied instance.
     * @param method Intercepted method.
     * @param args Array of arguments, primitive types are boxed.
     * @param context Map of interception contextual parameters.
     */
    public void before(T proxy, Method method, Object[] args, Map<String, Object> context) {
        //Do nothing
    }

    /**
     * Action (advice) to be executed <b>after</b> execution of the original method.
     * @param proxy The proxied instance.
     * @param method Intercepted method.
     * @param args Array of arguments, primitive types are boxed.
     * @param context Map of interception contextual parameters.
     */
    public void after(T proxy, Method method, Object[] args, Map<String, Object> context) {
        //Do nothing
    }

    /**
     * Handle exception caused by:
     * <ul>
     *     <li>Target method code itself.</li>
     *     <li>Invocation of the original method. These exceptions won't be wrapped into {@link java.lang.reflect.InvocationTargetException}.</li>
     * </ul>
     * <p>
     *     The implementation is not allowed to throw a checked exception. Exceptional behavior should be expressed by
     *     returning a result.
     * </p>
     *
     * @param proxy The proxied instance.
     * @param method Intercepted method.
     * @param args Array of arguments, primitive types are boxed.
     * @param cause The original exception (throwable).
     * @param context
     * @return The resulting exception to be thrown.
     */
    public Throwable handleException(T proxy, Method method, Object[] args, Throwable cause, Map<String, Object> context) {
        //Just return the cause
        return cause;
    }
}
