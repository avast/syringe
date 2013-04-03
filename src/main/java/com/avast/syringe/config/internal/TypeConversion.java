package com.avast.syringe.config.internal;

import java.util.Map;

import com.avast.syringe.config.ConfigException;
import com.google.common.collect.Maps;
import com.google.common.primitives.Primitives;

public class TypeConversion {

    private static final Map<Class<?>, TypeConverter<?>> registry = Maps.newHashMap();

    static {
        registry.put(String.class, new StringConverter());
        registry.put(Boolean.class, new BooleanConverter());
        registry.put(boolean.class, new BooleanConverter());
        registry.put(Integer.class, new IntegerConverter());
        registry.put(int.class, new IntegerConverter());
        registry.put(Long.class, new LongConverter());
        registry.put(long.class, new LongConverter());
        registry.put(Class.class, new ClassConverter());
    }

    @SuppressWarnings("unchecked")
    private static <T> TypeConverter<T> findConverter(Class<T> clazz) {
        return (TypeConverter<T>) registry.get(clazz);
    }

    private static <T> TypeConverter<T> getConverter(Class<T> clazz) {
        TypeConverter<T> converter = findConverter(clazz);
        if (converter == null) {
            String message = String.format("Conversion to class %s is not supported", clazz);
            throw new IllegalArgumentException(message);
        }
        return converter;
    }

    public static <T> String convertToString(T value, Class<T> clazz) throws Exception {
        TypeConverter<T> converter = getConverter(clazz);
        return converter.convertToString(value);
    }

    public static <T> T convert(String value, Class<T> clazz) throws Exception {
        if (clazz.isAssignableFrom(String.class)) {
            return clazz.cast(value);
        }
        clazz = Primitives.wrap(clazz);
        TypeConverter<T> converter = getConverter(clazz);
        return converter.convert(value);
    }

    public static String getXmlSchemaTypeName(Class<?> clazz) {
        if (clazz == String.class) {
            return "xs:string";
        }
        clazz = Primitives.wrap(clazz);
        TypeConverter<?> converter = findConverter(clazz);
        if (converter != null) {
            return converter.getXmlSchemaTypeName();
        } else {
            return clazz.getName();
        }
    }

    private static interface TypeConverter<T> {

        T convert(String value) throws Exception;

        String convertToString(T value) throws Exception;

        // Sort of ugly to have this in a general-purpose TypeConverted interface,
        // but good enough for now.
        String getXmlSchemaTypeName();
    }

    private static class BooleanConverter implements TypeConverter<Boolean> {

        @Override
        public Boolean convert(String value) {
            // Can't use Boolean.parseBoolean() because XML Schema also supports "0" and "1".
            return value.equalsIgnoreCase("true") || value.equals("1");
        }

        @Override
        public String convertToString(Boolean value) {
            return value.toString();
        }

        @Override
        public String getXmlSchemaTypeName() {
            return "xs:boolean";
        }
    }

    private static class IntegerConverter implements TypeConverter<Integer> {

        @Override
        public Integer convert(String value) {
            return Integer.parseInt(value);
        }

        @Override
        public String convertToString(Integer value) {
            return value.toString();
        }

        @Override
        public String getXmlSchemaTypeName() {
            return "xs:integer";
        }

    }

    private static class LongConverter implements TypeConverter<Long> {

        @Override
        public Long convert(String value) {
            return Long.parseLong(value);
        }

        @Override
        public String convertToString(Long value) {
            return value.toString();
        }

        @Override
        public String getXmlSchemaTypeName() {
            return "xs:integer";
        }
    }

    private static class StringConverter implements TypeConverter<String> {

        @Override
        public String convert(String value) {
            return value;
        }

        @Override
        public String convertToString(String value) {
            return value;
        }

        @Override
        public String getXmlSchemaTypeName() {
            return "xs:string";
        }
    }

    private static class ClassConverter implements TypeConverter<Class<?>> {

        @Override
        public Class<?> convert(String value) throws ClassNotFoundException {
            return TypeConversion.class.getClassLoader().loadClass(value);
        }

        @Override
        public String convertToString(Class<?> value) {
            return value.getName();
        }

        @Override
        public String getXmlSchemaTypeName() {
            return "xs:string";
        }
    }
}
