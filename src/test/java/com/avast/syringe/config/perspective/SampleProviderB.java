package com.avast.syringe.config.perspective;

import com.avast.syringe.*;
import com.avast.syringe.config.ConfigBean;
import com.avast.syringe.config.ConfigProperty;

import java.util.Map;

/**
 * User: slajchrt
 * Date: 7/19/12
 * Time: 11:46 AM
 */
@ConfigBean
public class SampleProviderB implements com.avast.syringe.Provider<Runnable> {

    @ConfigProperty(optional = true)
    private int iProp;

    @Override
    public Runnable getInstance() {
        return null;
    }
}
