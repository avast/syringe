package com.avast.syringe.aop;

import java.lang.reflect.Method;

/**
 * User: vacata
 * Date: 1/29/13
 * Time: 11:40 AM
 */
public interface MethodPointcut {

    boolean accept(Method method);
}
