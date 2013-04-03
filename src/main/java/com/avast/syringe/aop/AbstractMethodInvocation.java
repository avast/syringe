package com.avast.syringe.aop;

import java.lang.reflect.Method;

/**
 * User: vacata
 * Date: 2/18/13
 * Time: 11:56 AM
 */
public abstract class AbstractMethodInvocation<T> implements MethodInvocation<T> {

    protected T rawTarget;
    protected T proxy;
    protected Method method;
    protected Object[] args;

    protected AbstractMethodInvocation(T rawTarget, T proxy, Method method, Object[] args) {
        this.rawTarget = rawTarget;
        this.proxy = proxy;
        this.method = method;
        this.args = args;
    }

    public T getRawTarget() {
        return rawTarget;
    }

    public T getProxy() {
        return proxy;
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getArgs() {
        return args;
    }
}
