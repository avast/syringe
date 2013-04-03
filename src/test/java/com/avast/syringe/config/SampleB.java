package com.avast.syringe.config;

/**
 * User: slajchrt
 * Date: 3/13/12
 * Time: 5:05 PM
 */
public class SampleB {

    @ConfigProperty
    private String x;

    @ConfigProperty
    private SampleA sa;

    public String getX() {
        return x;
    }

    public SampleA getSa() {
        return sa;
    }
}
