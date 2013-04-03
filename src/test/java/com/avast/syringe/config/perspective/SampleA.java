package com.avast.syringe.config.perspective;

import com.avast.syringe.config.ConfigBean;
import com.avast.syringe.config.ConfigProperty;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: slajchrt
 * Date: 6/4/12
 * Time: 6:17 PM
 */
@ConfigBean
public class SampleA implements Runnable {

    public static final AtomicInteger initSequence = new AtomicInteger();

    @ConfigProperty(optional = true, tags = {"i"})
    private int iProp;

    @ConfigProperty(optional = false, tags = {"s"})
    private String sProp;

    @ConfigProperty(optional = true)
    private List<String> lProp;

    @ConfigProperty(optional = true)
    private String[] aProp;

    @ConfigProperty(optional = true)
    private Map<String, Integer> mProp;

    @ConfigProperty(optional = true)
    private Runnable rProp;

    @ConfigProperty(optional = true)
    private Runnable[] arProp;

    @ConfigProperty(optional = true)
    private List<Runnable> lrProp;

    @ConfigProperty(optional = true)
    private Map<Runnable, Runnable> mrProp;

    public int getiProp() {
        return iProp;
    }

    public String getsProp() {
        return sProp;
    }

    public List<String> getlProp() {
        return lProp;
    }

    public void setlProp(List<String> lProp) {
        this.lProp = lProp;
    }

    public String[] getaProp() {
        return aProp;
    }

    public Map<String, Integer> getmProp() {
        return mProp;
    }

    public Runnable getrProp() {
        return rProp;
    }

    public Runnable[] getARProp() {
        return arProp;
    }

    public List<Runnable> getLRProp() {
        return lrProp;
    }

    public Map<Runnable, Runnable> getMRProp() {
        return mrProp;
    }

    @Override
    public void run() {
    }

    int initOrder;

    @PostConstruct
    public void init() {
        initOrder = initSequence.incrementAndGet();
    }
}
