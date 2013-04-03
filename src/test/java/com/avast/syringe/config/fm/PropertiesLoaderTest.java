package com.avast.syringe.config.fm;

import junit.framework.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.Properties;

/**
 * User: slajchrt
 * Date: 4/12/12
 * Time: 5:38 PM
 */
public class PropertiesLoaderTest {

    @Test
    public void testEnsureArraySize() {
        Object[] arr = new Object[0];
        arr = PropertiesLoader.ensureArraySize(3, arr);
        Assert.assertEquals(4, arr.length);

        arr = new Object[3];
        arr = PropertiesLoader.ensureArraySize(3, arr);
        Assert.assertEquals(4, arr.length);

        arr = new Object[3];
        Assert.assertSame(arr, PropertiesLoader.ensureArraySize(1, arr));

        arr = new Object[3];
        Assert.assertSame(arr, PropertiesLoader.ensureArraySize(2, arr));

        arr = PropertiesLoader.ensureArraySize(3, null);
        Assert.assertEquals(4, arr.length);

    }

    @Test
    public void testLoadScalarProperties() {
        Properties props = new Properties();
        props.setProperty("prop1", "abc");
        props.setProperty("prop2", "xyz");
        Map map = PropertiesLoader.load(props);

        Assert.assertEquals(map.get("prop1"), "abc");
        Assert.assertEquals(map.get("prop2"), "xyz");
    }

    @Test
    public void testLoadArrayProperties() {
        Properties props = new Properties();
        props.setProperty("prop1[0]", "aaa");
        props.setProperty("prop1[2]", "ccc");
        props.setProperty("prop1[1]", "bbb");
        Map map = PropertiesLoader.load(props);

        Object[] val = (Object[]) map.get("prop1");
        Assert.assertEquals(3, val.length);
        Assert.assertEquals("aaa", val[0]);
        Assert.assertEquals("bbb", val[1]);
        Assert.assertEquals("ccc", val[2]);
    }
}
