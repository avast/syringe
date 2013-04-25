package com.avast.syringe.config.internal;

import com.avast.syringe.config.ConfigProperty;
import junit.framework.TestCase;

/**
 * User: zslajchrt
 * Date: 4/25/13
 * Time: 2:29 PM
 */
public class ConfigClassAnalyzerTest extends TestCase {

    static interface I {

    }

    static class A implements I {

    }

    static class B implements I {
        @ConfigProperty(delegate = true)
        I i;
    }

    public void testStripShallow() {
        A a = new A();
        B b = new B();
        b.i = a;

        Object stripped = ConfigClassAnalyzer.stripShallow(b);
        assertSame(a, stripped);
    }

    public void testStripDeep() {
        A a = new A();
        B b1 = new B();
        B b2 = new B();
        b1.i = a;
        b2.i = b1;

        Object stripped = ConfigClassAnalyzer.stripDeep(b2);
        assertSame(a, stripped);
        stripped = ConfigClassAnalyzer.stripDeep(b1);
        assertSame(a, stripped);
    }
}
