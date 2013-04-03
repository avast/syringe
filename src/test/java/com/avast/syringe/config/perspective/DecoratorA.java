package com.avast.syringe.config.perspective;

import com.avast.syringe.config.ConfigProperty;

import javax.annotation.PostConstruct;

/**
 * User: slajchrt
 * Date: 6/14/12
 * Time: 11:16 AM
 */
public class DecoratorA implements Runnable {

    @ConfigProperty(delegate = true)
    Runnable delegate;

    @ConfigProperty(optional = true)
    int id;

    @Override
    public void run() {
        delegate.run();
    }

    int initOrder;

    @PostConstruct
    public void init() {
        initOrder = SampleA.initSequence.incrementAndGet();
    }

}
