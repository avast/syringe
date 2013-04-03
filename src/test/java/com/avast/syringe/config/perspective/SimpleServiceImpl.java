package com.avast.syringe.config.perspective;

import com.avast.syringe.config.ConfigBean;

/**
 * User: vacata
 * Date: 1/30/13
 * Time: 2:30 PM
 */
@ConfigBean
public class SimpleServiceImpl implements SimpleService {
    @Override
    public void doSomething() {
        System.out.println("Doing something.");
    }
}
