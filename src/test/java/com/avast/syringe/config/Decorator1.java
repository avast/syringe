package com.avast.syringe.config;

/**
 * User: slajchrt
 * Date: 5/31/12
 * Time: 7:28 PM
 */
public class Decorator1 implements Runnable {

    @ConfigProperty(delegate = true)
    private Runnable delegate;

    @ConfigProperty
    private String a;

    @Override
    public void run() {
        delegate.run();
    }

    public Runnable getDelegate() {
        return delegate;
    }

}
