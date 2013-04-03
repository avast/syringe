package com.avast.syringe.aop.cglib;

import com.avast.syringe.aop.Interceptor;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * User: vacata
 * Date: 1/29/13
 * Time: 4:17 PM
 */
class DefaultMethodInterceptor implements MethodInterceptor {

    private static Logger logger = LoggerFactory.getLogger(DefaultMethodInterceptor.class);

    private Interceptor interceptor;

    DefaultMethodInterceptor(Interceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        logger.debug("Creating new instance of method invocation.");
        CglibMethodInvocation methodInvocation = new CglibMethodInvocation(interceptor.getTarget(), o, method, objects, methodProxy);
        logger.debug("Calling interceptor {}.", interceptor);
        return interceptor.intercept(methodInvocation);
    }
}
