package com.avast.syringe.config;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Wraps {@link java.util.concurrent.atomic.AtomicReference} to allow intercepting setting the value.
 * <p/>
 * User: slajchrt
 * Date: 4/5/12
 * Time: 3:00 PM
 */
public class MutableReference<T> {

    private final AtomicReference<T> reference = new AtomicReference<T>();

    public MutableReference() {
    }

    public MutableReference(T initValue) {
        reference.set(initValue);
    }

    public T get() {
        return reference.get();
    }

    public void set(T value) throws Exception {
        beforeSet(value);
        reference.set(value);
        afterSet(value);
    }

    /**
     * @param value the new value
     * @throws Exception throws an exception to prevent setting the new value
     */
    protected void beforeSet(T value) throws Exception {
    }

    protected void afterSet(T value) {
    }

    @Override
    public String toString() {
        return reference.toString();
    }
}
