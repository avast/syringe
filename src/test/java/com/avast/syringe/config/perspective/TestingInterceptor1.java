package com.avast.syringe.config.perspective;

import com.avast.syringe.aop.AroundInterceptor;
import com.avast.syringe.config.ConfigBean;
import com.avast.syringe.config.ConfigProperty;

import java.lang.reflect.Method;
import java.util.Map;

/**
* User: vacata
* Date: 1/30/13
* Time: 2:35 PM
*/
@ConfigBean
class TestingInterceptor1 extends AroundInterceptor<SimpleService> {

    @ConfigProperty
    private InterceptCounter interceptCounter;

    public InterceptCounter getInterceptCounter() {
        return interceptCounter;
    }

    @Override
    public void before(SimpleService proxy, Method method, Object[] args, Map<String, Object> context) {
        System.out.println("Before intercepted method.");
        if (interceptCounter != null) {
            interceptCounter.incrementBefore();
        }
    }

    @Override
    public void after(SimpleService proxy, Method method, Object[] args, Map<String, Object> context) {
        System.out.println("After intercepted method.");
        if (interceptCounter != null) {
            interceptCounter.incrementAfter();
        }
    }

    public static class InterceptCounter {
        private int beforeCount = 0;
        private int afterCount = 0;

        public void incrementBefore() {
            beforeCount++;
        }

        public int getBeforeCount() {
            return beforeCount;
        }

        public void incrementAfter() {
            afterCount++;
        }

        public int getAfterCount() {
            return afterCount;
        }
    }
}
