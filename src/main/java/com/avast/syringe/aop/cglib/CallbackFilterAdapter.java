package com.avast.syringe.aop.cglib;

import com.avast.syringe.aop.MethodPointcut;
import net.sf.cglib.proxy.CallbackFilter;

import java.lang.reflect.Method;

/**
 * User: vacata
 * Date: 1/29/13
 * Time: 2:48 PM
 */
public class CallbackFilterAdapter implements CallbackFilter {

    private MethodPointcut pointcut;

    public CallbackFilterAdapter(MethodPointcut pointcut) {
        this.pointcut = pointcut;
    }

    @Override
    public int accept(Method method) {
        if (pointcut.accept(method)) {
            return 0;
        } else {
            return 1;
        }
    }
}
