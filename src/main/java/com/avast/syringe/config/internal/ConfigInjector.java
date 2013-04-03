package com.avast.syringe.config.internal;

import com.avast.syringe.config.ConfigException;
import com.avast.syringe.config.PropertyValueConverter;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class ConfigInjector<T> {

    private final Class<T> configClass;
    private final List<Injection> injections;
    private final Method postConstruct;
    private final Method preDestroy;

    public static <T> ConfigInjector<T> forClass(Class<T> configClass) {
        return forClass(configClass, null);
    }

    public static <T> ConfigInjector<T> forClass(Class<T> configClass, @Nullable PropertyValueConverter converter) {
        return new ConfigInjector<T>(configClass, converter);
    }

    private ConfigInjector(Class<T> configClass, PropertyValueConverter converter) {
        this.configClass = configClass;

        injections = Lists.newArrayList();
        ConfigClassAnalyzer configClassAnalyzer = new ConfigClassAnalyzer(configClass, converter);
        for (InjectableProperty property : configClassAnalyzer.getConfigProperties()) {
            injections.add(new Injection(property));
        }

        postConstruct = configClassAnalyzer.findPostConstructMethod();
        preDestroy = configClassAnalyzer.findPreDestroyMethod();
    }

    public Class<T> getConfigClass() {
        return configClass;
    }

    public T inject(Map<String, Property> props) throws Exception {
        return inject(props, Injection.DEFAULT_CONTEXTUAL_PROPERTY_RESOLVER);
    }

    public T inject(Map<String, Property> props, Injection.ContextualPropertyResolver resolver) throws Exception {
            T bean = createInstance();
            for (Injection injection : injections) {
                try {
                    injection.apply(bean, props, resolver);
                } catch (ConfigException e) {
                    throw e;
                } catch (Exception e) {
                    throw new ConfigException(injection.getProperty().getName(), bean.getClass().getName(), "", e);
                }
            }
            return bean;
    }

    public List<Injection> getInjections() {
        return injections;
    }

    public boolean containsMandatory() {
        for (Injection injection : injections) {
            if (!injection.getProperty().isOptional()) {
                return true;
            }
        }

        return false;
    }

    private T createInstance() throws Exception {
        return createInstance(configClass);
    }

    static <T> T createInstance(Class<T> clazz) throws Exception {
            return clazz.newInstance();
    }

    public void notifyPostConstruct(Object instance) throws Exception {
        if (postConstruct != null) {
            postConstruct.invoke(instance);
        }
    }

    public void notifyPreDestroy(Object instance) throws Exception {
        if (preDestroy != null) {
            preDestroy.invoke(instance);
        }
    }

}
