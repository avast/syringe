package com.avast.syringe.aop;

import java.lang.reflect.Method;

/**
 * User: vacata
 * Date: 1/29/13
 * Time: 5:33 PM
 */
public class InterceptAllPointcut implements MethodPointcut {

    @Override
    public boolean accept(Method method) {
        //Simply intercept everything
        return true;
    }
}
