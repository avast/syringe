package com.avast.syringe;

import junit.framework.Assert;
import org.junit.Test;

/**
 * User: slajchrt
 * Date: 6/22/12
 * Time: 11:24 AM
 */
public class SingletonProviderFactoryTest {

    public static interface SpecialProvider<T> extends Provider<T> {

        int getNumber();

    }

    public static class P1 implements SpecialProvider<String> {
        @Override
        public String getInstance() throws Exception {
            return new String("A");
        }

        @Override
        public int getNumber() {
            return 10;
        }
    }

    public static class P2 extends P1 implements Runnable {
        int n;

        @Override
        public void run() {
            n = 20;
        }

        @Override
        public int getNumber() {
            return n == 0 ? super.getNumber() : n;
        }
    }

    @Test
    public void testCreateSingletonProxy() throws Exception {
        P2 provider = new P2();

        Assert.assertNotSame(provider.getInstance(), provider.getInstance());

        Object singletonProvider = SingletonProviderFactory.createSingletonProvider(provider);

        Assert.assertTrue(singletonProvider instanceof Runnable);
        Assert.assertTrue(singletonProvider instanceof SpecialProvider);

        SpecialProvider sp = (SpecialProvider)singletonProvider;

        Assert.assertSame(sp.getInstance(), sp.getInstance());

        Runnable r = (Runnable)singletonProvider;
        Assert.assertEquals(10, sp.getNumber());
        r.run();
        Assert.assertEquals(20, sp.getNumber());


    }

}
