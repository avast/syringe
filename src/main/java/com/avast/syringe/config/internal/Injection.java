package com.avast.syringe.config.internal;

import com.avast.syringe.config.ConfigException;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Performs the actual injection of a {@link Property} into an injectable property
 * of a given instance.
 */
public class Injection {

    public interface ContextualPropertyResolver {
        Object getProperty(InjectableProperty property, Value configValue) throws Exception;
    }

    public static final ContextualPropertyResolver DEFAULT_CONTEXTUAL_PROPERTY_RESOLVER = new ContextualPropertyResolver() {
        @Override
        public Object getProperty(InjectableProperty property, Value configValue) throws Exception {
            throw new NoSuchFieldException(property.getName());
        }
    };

    private final InjectableProperty property;

    public Injection(InjectableProperty property) {
        this.property = property;
    }

    /**
     * Takes the property from the {@code properties} map, converts it to the
     * correct value and sets it to the {@code instance}'s property.
     */
    public void apply(Object instance, Map<String, Property> properties, ContextualPropertyResolver resolver)
            throws Exception {
        if (property.isContextual() || property.isReference()) {
            applyContextual(instance, properties, resolver);
        } else {
            applyNonContextual(instance, properties, resolver);
        }
    }

    private void applyContextual(Object instance, Map<String, Property> properties, ContextualPropertyResolver resolver) throws Exception {
        Value configValue = getConfigValue(properties);
        Object propVal;
        try {
            propVal = resolver.getProperty(property, configValue);
        } catch (NoSuchFieldException e) {
            throwNotConfigured(e);
            return;
        }
        property.setValue(instance, propVal);
    }

    private void throwNotConfigured(NoSuchFieldException e) throws NoSuchFieldException {
        if (!property.isOptional()) {
            String message = String.format("Required property %s not configured", property.getName());
            throw e == null ? new NoSuchFieldException(message) : e;
        }
    }

    private Value getConfigValue(Map<String, Property> properties) throws Exception {
        Property configProp = properties.get(property.getName());
        Value configValue;
        if (configProp == null) {
            configValue = null;
        } else {
            try {
                configValue = configProp.getValues().get(0);
            } catch (Exception e) {
                throw e;
            }
        }
        return configValue;
    }

    public InjectableProperty getProperty() {
        return property;
    }

    private void applyNonContextual(Object instance, Map<String, Property> properties,
                                    ContextualPropertyResolver resolver) throws Exception {
        String propName = property.getName();
        Property prop = properties.get(propName);

        if (prop == null) {

            if (resolver != null) {
                applyContextual(instance, properties, resolver);
                return;
            } else {
                throwNotConfigured(null);
                return;
            }

        }

        if (property.isArray()) {
            injectArray(instance, prop, resolver);
        } else if (property.isCollection()) {
            Class<?> type = property.getType();
            if (List.class.isAssignableFrom(type)) {
                injectCollection(instance, prop, ArrayList.class, resolver);
            } else if (Set.class.isAssignableFrom(type)) {
                injectCollection(instance, prop, HashSet.class, resolver);
            } else {
                injectCollection(instance, prop, null, resolver);
            }
        } else if (property.isMap()) {
            injectMap(instance, prop, resolver);
        } else {
            injectSimple(instance, prop, resolver);
        }
    }

    private void injectSimple(Object instance, Property prop, ContextualPropertyResolver resolver) throws Exception {
        List<Value> values = prop.getValues();
        if (values.size() > 1) {
            String message = String.format("Property %s takes a single value, but was configured with %s", prop.getName(), prop.getValues());
            throw new ConfigException(prop, instance.getClass(), message, null);
        }

        Value value = prop.getValues().get(0);
        Object val;
        String refType = value.getRefType();
        if (refType != null) {
            val = resolver.getProperty(property, value);
        } else {
            val = TypeConversion.convert(value.getValue(), property.getType());
        }

        property.setValue(instance, val);
    }

    private void injectArray(Object instance, Property prop, ContextualPropertyResolver resolver) throws Exception {
        Class<?> componentType = property.getArrayOrCollectionComponentType();
        if (componentType.isArray()) {
            String message = String.format("Multi-dimensional array property %s is not supported. Only single-dimensional arrays are supported",
                    property.getName());
            throw new ConfigException(prop, instance.getClass(), message, null);
        }

        Object array = property.getValue(instance);
        if (array == null) {
            array = Array.newInstance(componentType, prop.getValues().size());
            property.setValue(instance, array);
        }

        for (int i = 0; i < prop.getValues().size(); i++) {
            Value value = prop.getValues().get(i);
            Object element;
            String refType = value.getRefType();
            if (refType != null) {
                element = resolver.getProperty(property, value);
            } else {
                element = TypeConversion.convert(value.getValue(), componentType);
            }
            property.setArrayElement(instance, i, element);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void injectCollection(Object instance, Property prop, Class<? extends Collection> defaultCollectionClass,
                                  ContextualPropertyResolver resolver) throws Exception {
        Collection<Object> collection = (Collection<Object>) property.getValue(instance);
        if (collection == null) {
            if (defaultCollectionClass == null) {
                String message = String.format("Cannot infer a collection implementation for property %s. Initialize it with a concrete empty collection",
                        property.getName());
                throw new ConfigException(prop, instance.getClass(), message, null);
            }
            collection = ConfigInjector.createInstance(defaultCollectionClass);
            property.setValue(instance, collection);
        }

        Class<?> componentType = property.getArrayOrCollectionComponentType();
        for (Value value : prop.getValues()) {
            Object element;
            String refType = value.getRefType();
            if (refType != null) {
                element = resolver.getProperty(property, value);
            } else {
                element = TypeConversion.convert(value.getValue(), componentType);
            }
            property.addCollectionElement(instance, element);
        }
    }

    @SuppressWarnings({"unchecked"})
    private void injectMap(Object instance, Property prop, ContextualPropertyResolver resolver) throws Exception {
        Map<Object, Object> map = (Map<Object, Object>) property.getValue(instance);
        if (map == null) {
            map = ConfigInjector.createInstance(HashMap.class);
            property.setValue(instance, map);
        }

        Class<?> keyType = property.getMapKeyType();
        Class<?> valueType = property.getMapValueType();
        for (Value value : prop.getValues()) {
            MapEntry entry = (MapEntry) value;
            Object mapKey = TypeConversion.convert(entry.getKey(), keyType);

            Object mapValue;
            String refType = value.getRefType();
            if (refType != null) {
                mapValue = resolver.getProperty(property, value);
            } else {
                mapValue = TypeConversion.convert(value.getValue(), valueType);
            }

            property.putMapEntry(instance, mapKey, mapValue);
        }
    }
    public static void inject(Object instance, String propertyName, Object value){
        InjectableProperty property = getInjectableProperty(instance.getClass(), propertyName);
        try {
            property.setValue(instance, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private static InjectableProperty getInjectableProperty(Class cls, String propertyName){
        ConfigClassAnalyzer configClassAnalyzer = new ConfigClassAnalyzer(cls);
        for(InjectableProperty  property : configClassAnalyzer.getConfigProperties()){
            if(property.getName().equals(propertyName))
                return property;
        }
        return null;
    }
}
