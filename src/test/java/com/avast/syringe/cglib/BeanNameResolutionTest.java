/*
 * Copyright (c) 2015 Avast a.s., www.avast.com
 */
package com.avast.syringe.cglib;

import com.avast.syringe.config.ConfigBean;
import com.avast.syringe.config.perspective.resolver.BeanNameResolution;
import com.google.common.base.Optional;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Named;

/**
 *
 * @author Jan Lastovicka (lastovicka@avast.com)
 * @since 2015-07-02
 */
public class BeanNameResolutionTest {

    private BeanNameResolution resolution;

    @Before
    public void init() {
        resolution = new BeanNameResolution();
    }

    @Test
    public void getExpectedDefaultBeanName() {
        Optional<String> name = resolution.resolve(TestedClass.class);

        Assert.assertTrue(name.isPresent());
        Assert.assertTrue(name.get().equals("TestedClass"));
    }

    @Test
    public void getsExpectedBeanNameFromConfigBeanAnnotation() {
        Optional<String> name = resolution.resolve(Tested2Class.class);

        Assert.assertTrue(name.isPresent());
        Assert.assertTrue(name.get().equals("Tested2Class"));
    }

    @Test
    public void getExpectedBeanNameFromConfigBeanAnnotation() {
        Optional<String> name = resolution.resolve(Tested3Class.class);

        Assert.assertTrue(name.isPresent());
        Assert.assertTrue(name.get().equals("hello"));
    }

    @Test
    public void getsExpectedDefaultBeanNameFromNamedAnnotation() {
        Optional<String> name = resolution.resolve(Tested4Class.class);

        Assert.assertTrue(name.isPresent());
        Assert.assertTrue(name.get().equals("Tested4Class"));
    }

    @Test
    public void getsExpecteBeanNameFromNamedAnnotation() {
        Optional<String> name = resolution.resolve(Tested5Class.class);

        Assert.assertTrue(name.isPresent());
        Assert.assertTrue(name.get().equals("bina"));
    }

    private static final class TestedClass {

    }

    @ConfigBean
    private static final class Tested2Class {

    }

    @ConfigBean("hello")
    private static final class Tested3Class {

    }

    @Named
    private static final class Tested4Class {

    }

    @Named("bina")
    private static final class Tested5Class {

    }

}
