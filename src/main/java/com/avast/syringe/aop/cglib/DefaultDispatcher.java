package com.avast.syringe.aop.cglib;

import net.sf.cglib.proxy.Dispatcher;

/**
 * User: vacata
 * Date: 8/30/13
 * Time: 4:32 PM
 *
 * Simple implementation of {@link Dispatcher} returning given instance. All method calls "proxied" by this Callback should
 * use given target to do the actual work.
 */
public class DefaultDispatcher implements Dispatcher {

    private Object target;

    public DefaultDispatcher(Object target) {
        this.target = target;
    }

    @Override
    public Object loadObject() throws Exception {
        return this.target;
    }
}
