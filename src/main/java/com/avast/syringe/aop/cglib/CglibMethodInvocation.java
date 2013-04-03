package com.avast.syringe.aop.cglib;

import com.avast.syringe.aop.AbstractMethodInvocation;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * User: vacata
 * Date: 2/18/13
 * Time: 11:57 AM
 */
public class CglibMethodInvocation<T> extends AbstractMethodInvocation<T> {

    private MethodProxy methodProxy;

    public CglibMethodInvocation(T rawTarget, T proxy, Method method, Object[] args, MethodProxy methodProxy) {
        super(rawTarget, proxy, method, args);
        this.methodProxy = methodProxy;
    }

    @Override
    public Object invoke() throws Throwable {
        return methodProxy.invoke(rawTarget, args);
    }
}
