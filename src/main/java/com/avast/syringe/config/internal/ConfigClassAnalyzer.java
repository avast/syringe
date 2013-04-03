package com.avast.syringe.config.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.avast.syringe.config.ConfigProperty;
import com.avast.syringe.config.PropertyValueConverter;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class ConfigClassAnalyzer {

    private final Class<?> configClass;
    private final PropertyValueConverter converter;

    public ConfigClassAnalyzer(Class<?> configClass) {
        this(configClass, null);
    }

    public ConfigClassAnalyzer(Class<?> configClass, @Nullable PropertyValueConverter converter) {
        this.configClass = configClass;
        this.converter = converter;
    }

    public static Method findAnnotatedMethod(Class<? extends Annotation> annotationClass, Class cls) {
        Method[] methods = cls.getMethods();
        for (Method method : methods) {
            Annotation annotation = method.getAnnotation(annotationClass);
            if (annotation != null) {
                return method;
            }
        }
        return null;
    }

    public Method findPostConstructMethod() {
        return findAnnotatedMethod(PostConstruct.class, configClass);
    }

    public Method findPreDestroyMethod() {
        return findAnnotatedMethod(PreDestroy.class, configClass);
    }

    public List<InjectableProperty> getConfigProperties() {
        List<InjectableProperty> result = Lists.newArrayList();

        LinkedList<Class<?>> classes = Lists.newLinkedList();
        Class<?> each = configClass;
        while (each != null) {
            // Put superclasses (and injectable properties thereof) first
            // in order to maintain their logical order.
            // Don't care about clazz.getInterfaces() because fields in an interface can only be static,
            // and we don't handle static fields.
            classes.addFirst(each);
            each = each.getSuperclass();
        }

        for (Class<?> clazz : classes) {
            for (Field field : clazz.getDeclaredFields()) {

                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                ConfigProperty configParam = field.getAnnotation(ConfigProperty.class);
                if (configParam != null) {
                    InjectableProperty property = new ReflectionInjectableProperty(field, configParam.optional(),
                            configParam.habitat(), configParam.delegate(), converter);
                    result.add(property);
                }

            }
        }
        return ImmutableList.copyOf(result);
    }

    public static Map<String, InjectableProperty> toMap(Class cls) {
        return toMap(new ConfigClassAnalyzer(cls).getConfigProperties());
    }

    public static Map<String, InjectableProperty> toMap(List<InjectableProperty> properties) {
        return Maps.uniqueIndex(properties, new Function<InjectableProperty, String>() {
            @Override
            public String apply(@Nullable InjectableProperty input) {
                return input.getName();
            }
        });
    }
}
