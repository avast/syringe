package com.avast.syringe.config;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

/**
 * User: slajchrt
 * Date: 3/13/12
 * Time: 5:05 PM
 */
public class SampleA {

    @ConfigProperty
    private int i;

    @ConfigProperty
    private String s = "abc";

    @ConfigProperty
    private List<String> l = Lists.newArrayList("abc", "xyz");

    @ConfigProperty
    private Map<Integer, String> m = Maps.newHashMap();
    {
        m.put(1, "ABC");
        m.put(2, "XYZ");
    }

    @ConfigProperty
    private List<Runnable> lr;

    @ConfigProperty
    private Map<Integer, Runnable> mr;

    @ConfigProperty
    private Runnable r1;

    @ConfigProperty
    private Runnable r2;

    @ConfigProperty
    private SampleC r3;

    public int getI() {
        return i;
    }

    public String getS() {
        return s;
    }

    public List<String> getL() {
        return l;
    }

    public Map<Integer, String> getM() {
        return m;
    }

    public List<Runnable> getLr() {
        return lr;
    }

    public Map<Integer, Runnable> getMr() {
        return mr;
    }

    public Runnable getR1() {
        return r1;
    }

    public Runnable getR2() {
        return r2;
    }

    public SampleC getR3() {
        return r3;
    }

    public void setR3(SampleC r3) {
        this.r3 = r3;
    }
}
