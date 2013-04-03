package com.avast.syringe.config.perspective;

import com.avast.syringe.*;
import com.avast.syringe.config.ConfigBean;
import com.avast.syringe.config.ConfigProperty;

import java.util.List;
import java.util.Map;

/**
 * User: slajchrt
 * Date: 7/19/12
 * Time: 11:46 AM
 */
@ConfigBean
public class SampleProviderA implements com.avast.syringe.Provider<Map<String, Integer>> {

    @ConfigProperty(optional = true)
    private int iProp;

    @Override
    public Map<String, Integer> getInstance() {
        return null;
    }
}
