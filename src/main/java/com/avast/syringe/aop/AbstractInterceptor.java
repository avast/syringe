package com.avast.syringe.aop;

import com.avast.syringe.config.ConfigBean;
import com.avast.syringe.config.ConfigProperty;

/**
 * User: vacata
 * Date: 1/31/13
 * Time: 2:57 PM
 */
@ConfigBean
public abstract class AbstractInterceptor<T> implements Interceptor<T> {

    @ConfigProperty(delegate = true)
    protected T rawTarget;

    @Override
    public T getTarget() {
        return rawTarget;
    }
}
