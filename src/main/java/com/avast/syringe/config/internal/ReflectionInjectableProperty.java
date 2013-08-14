package com.avast.syringe.config.internal;

import com.avast.syringe.Provider;
import com.avast.syringe.config.ConfigProperty;
import com.avast.syringe.config.MutableReference;
import com.avast.syringe.config.PropertyValueConverter;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Primitives;

import javax.annotation.Nullable;
import java.lang.reflect.*;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: slajchrt
 * Date: 4/2/12
 * Time: 3:10 PM
 */
public class ReflectionInjectableProperty implements InjectableProperty {

    // Right now we only support fields.
    private final Field field;
    private final Method atomicSetter;
    private final Method atomicGetter;
    private final boolean optional;
    private final String name;
    private final boolean delegate;
    private final ConfigProperty.Habitat habitat;
    private final PropertyValueConverter converter;
    private final Class referenceType;

    public ReflectionInjectableProperty(Field field, boolean optional, String name, ConfigProperty.Habitat habitat,
                                        boolean delegate, @Nullable PropertyValueConverter converter) {
        this.field = field;
        this.optional = optional;
        this.name = name;
        this.habitat = habitat;
        this.converter = converter;
        this.delegate = delegate;

        if (isAtomic()) {
            Preconditions.checkArgument(Modifier.isFinal(field.getModifiers()), "Property %s in %s must be final",
                    field.getName(), field.getDeclaringClass().getName());
            try {
                Class setterArgType = isAtomicReference() ? Object.class : getType();
                atomicSetter = field.getType().getMethod("set", setterArgType);
                atomicGetter = field.getType().getMethod("get");
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        } else {
            atomicSetter = null;
            atomicGetter = null;
        }

        if (isAtomicReference()) {
            // Determine the reference type
            Type refType;
            try {
                Class fieldClass = field.getType();
                Method getRefTypeMethod = fieldClass.getMethod("getReferenceType", Field.class);
                refType = (Type) getRefTypeMethod.invoke(null, field);
            } catch (NoSuchMethodException e) {
                refType = resolveRefType(field);
            } catch (IllegalAccessException e) {
                refType = resolveRefType(field);
            } catch (InvocationTargetException e) {
                refType = resolveRefType(field);
            }

            if (refType instanceof ParameterizedType) {
                Type rawRefType = ((ParameterizedType) refType).getRawType();

                if (rawRefType instanceof Class) {
                    referenceType = (Class) rawRefType;
                } else {
                    throw new UnsupportedOperationException("Unsupported reference type: " + refType);
                }

            } else if (refType instanceof Class) {
                referenceType = (Class) refType;
            } else {
                throw new UnsupportedOperationException("Unsupported reference type: " + refType);
            }

        } else {
            referenceType = null;
        }
    }

    private Type resolveRefType(Field field) {
        Type fieldGenericType = field.getGenericType();
        if (!(fieldGenericType instanceof ParameterizedType)) {
            throw new UnsupportedOperationException("Only simple generic type supported. Found: " + fieldGenericType);
        }

        ParameterizedType parameterizedType = (ParameterizedType) fieldGenericType;
        Preconditions.checkArgument(parameterizedType.getActualTypeArguments().length == 1,
                "Type %s of reference %s can have only one generic parameter", parameterizedType, field.getName());

        return parameterizedType.getActualTypeArguments()[0];
    }

    public boolean isContextual() {
        return habitat == ConfigProperty.Habitat.CONTEXT;
    }

    public static boolean isReference(Class<?> type) {
        if (isArray(type) ||
                isCollection(type) ||
                isMap(type) ||
                type.isPrimitive() ||
                Primitives.isWrapperType(type) ||
                Number.class.isAssignableFrom(type) ||
                AtomicBoolean.class.isAssignableFrom(type) ||
                Void.class == type ||
                String.class == type) {
            return false;
        }
        return true;
    }

    public boolean isDelegate() {
        return delegate;
    }

    public boolean isReference() {
        return isReference(getType());
    }

    public String getName() {
        if (name != null && name.length() > 0) {
            return this.name;
        } else {
            return field.getName();
        }
    }

    @Override
    public String getXmlSchemaTypeName() {
        return TypeConversion.getXmlSchemaTypeName(getType());
    }

    public Class<?> getType() {
        if (isAtomic()) {
            if (AtomicBoolean.class.isAssignableFrom(field.getType())) {
                return boolean.class;
            }

            if (AtomicInteger.class.isAssignableFrom(field.getType())) {
                return int.class;
            }

            if (AtomicLong.class.isAssignableFrom(field.getType())) {
                return long.class;
            }

            if (isAtomicReference()) {
                return referenceType;
            }

            throw new UnsupportedOperationException("Unsupported atomic type: " + field.getType());
        } else {
            return field.getType();
        }
    }


    public boolean isOptional() {
        return optional || delegate;
    }

    public static boolean isArray(Class type) {
        return type.isArray();
    }

    public boolean isArray() {
        return isArray(getType());
    }

    public boolean isAtomic() {
        return AtomicBoolean.class.isAssignableFrom(field.getType()) ||
                AtomicInteger.class.isAssignableFrom(field.getType()) ||
                AtomicLong.class.isAssignableFrom(field.getType()) ||
                isAtomicReference();
    }

    public boolean isAtomicReference() {
        return AtomicReference.class.isAssignableFrom(field.getType()) ||
                MutableReference.class.isAssignableFrom(field.getType());
    }

    public Class getAtomicType() {
        return isAtomic() ? field.getType() : null;
    }

    public static boolean isCollection(Class type) {
        return Collection.class.isAssignableFrom(type);
    }

    public boolean isCollection() {
        return isCollection(getType());
    }

    public static boolean isMap(Class type) {
        return Map.class.isAssignableFrom(type);
    }

    public boolean isMap() {
        return isMap(getType());
    }

    /**
     * For collection- and array-typed properties (i.e., only applies when
     * {@link #isCollection()} or {@link #isArray()} is {@code true}.
     */
    public Class<?> getArrayOrCollectionComponentType() {
        if (!isCollection() && !isArray()) {
            throw new IllegalStateException();
        }

        if (isCollection()) {
            Type type = field.getGenericType();
            if (type instanceof ParameterizedType) {
                return toClass(((ParameterizedType) type).getActualTypeArguments()[0]);
            } else {
                String message = String.format("Cannot determine the type parameter of the collection-typed field %s. Raw types are not supported", field);
                throw new IllegalArgumentException(message);
            }
        } else {
            return field.getType().getComponentType();
        }
    }

    /**
     * For map-typed properties (i.e., only applies when {@link #isMap()} is {@code true}.
     *
     * @return
     */
    public Class<?> getMapKeyType() {
        return toClass(getMapTypes()[0]);
    }

    /**
     * For map-typed properties (i.e., only applies when {@link #isMap()} is {@code true}.
     *
     * @return
     */
    public Class<?> getMapValueType() {
        return toClass(getMapTypes()[1]);
    }

    private Type[] getMapTypes() {
        if (!isMap()) {
            throw new IllegalStateException();
        }

        Type type = field.getGenericType();
        if (type instanceof ParameterizedType) {
            return ((ParameterizedType) type).getActualTypeArguments();
        } else {
            String message = String.format("Cannot determine one of the type parameters of the map-typed field %s. Raw types are not supported", field);
            throw new IllegalArgumentException(message);
        }
    }

    private static Class<?> toClass(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        }
        if (type instanceof WildcardType) {
            Type[] bounds = ((WildcardType) type).getLowerBounds();
            if (bounds.length == 0) {
                bounds = ((WildcardType) type).getUpperBounds();
            }
            if (bounds.length == 1) {
                return toClass(bounds[0]);
            }
        }
        String message = String.format("Conversion of type %s is not supported", type);
        throw new IllegalArgumentException(message);
    }

    public Object getValue(Object instance) throws Exception {
        field.setAccessible(true);
        if (isAtomic()) {
            Object atomicValue = field.get(instance);
            return atomicGetter.invoke(atomicValue);
        } else {
            return field.get(instance);
        }

    }

    public void setValue(Object instance, Object value) throws Exception {
        value = getInstanceFromProvider(value, getType());

        if (converter != null) {
            value = converter.convertTo(this, instance, getType(), value);
        }

        field.setAccessible(true);
        if (isAtomic()) {

            if (isAtomicReference() && value != null) {
                Preconditions.checkArgument(
                        referenceType.isInstance(value), "Incompatible instance %s for reference %s",
                        value.getClass(), field.getName());
            }

            // The field must be final, ie. it is assumed its value is not null
            Object atomicValue = field.get(instance);
            Preconditions.checkNotNull(atomicValue, "Atomic field must be final and not null");

            atomicSetter.invoke(atomicValue, value);
        } else {
            field.set(instance, value);
        }

    }

    private Object getInstanceFromProvider(Object value, Class type) throws Exception {
        // Try to cast to Provider and use its product
        if (value instanceof Provider && !isProvider(type)
                && !type.isInstance(value) // allow injecting providers
                ) {
            value = ((Provider) value).getInstance();

            if (value != null && !type.isInstance(value)) {
                throw new IllegalArgumentException("Cannot assign " + value.getClass().getName() + " instance to "
                        + getType().getName());
            }
        }
        return value;
    }

    private boolean isProvider(Class type) {
        return Provider.class.isAssignableFrom(type);
    }

    @Override
    public void putMapEntry(Object instance, Object mapKey, Object mapValue) throws Exception {
        if (converter != null) {
            mapKey = getInstanceFromProvider(mapKey, getMapKeyType());
            mapKey = converter.convertTo(this, instance, getMapKeyType(), mapKey);

            mapValue = getInstanceFromProvider(mapValue, getMapValueType());
            mapValue = converter.convertTo(this, instance, getMapValueType(), mapValue);
        }

        if (!isInstanceOf(getMapKeyType(), mapKey)) {
            throw new IllegalArgumentException("key type mismatch in map property " + getName() +
                    " of class " + field.getDeclaringClass().getName());
        }

        if (!isInstanceOf(getMapValueType(), mapValue)) {
            throw new IllegalArgumentException("Value type mismatch in property '" + getName() +
                    "' of class " + field.getDeclaringClass() + ". Found: " + mapValue + ", required:" + getMapValueType());
        }

        ((Map) getValue(instance)).put(mapKey, mapValue);
    }

    @Override
    public void addCollectionElement(Object instance, Object element) throws Exception {
        if (converter != null) {
            element = getInstanceFromProvider(element, getArrayOrCollectionComponentType());
            element = converter.convertTo(this, instance, getArrayOrCollectionComponentType(), element);
        }

        if (!isInstanceOf(getArrayOrCollectionComponentType(), element)) {
            throw new IllegalArgumentException("Collection element type mismatch in property '" + getName() +
                    "' of class " + field.getDeclaringClass() + ". Found: " + element + ", required:" + getArrayOrCollectionComponentType());
        }

        ((Collection) getValue(instance)).add(element);
    }

    @Override
    public void setArrayElement(Object instance, int i, Object element) throws Exception {
        element = getInstanceFromProvider(element, getArrayOrCollectionComponentType());
        if (converter != null) {
            element = converter.convertTo(this, instance, getArrayOrCollectionComponentType(), element);
        }
        Object array = getValue(instance);

        if (!isInstanceOf(getArrayOrCollectionComponentType(), element)) {
            throw new IllegalArgumentException("Array element type mismatch in property '" + getName() +
                    "' of class " + field.getDeclaringClass() + ". Found: " + element + ", required:" + getArrayOrCollectionComponentType());
        }

        Array.set(array, i, element);
    }

    @Override
    public boolean hasTag(String tag) {
        ConfigProperty configPropertyAnnotation = field.getAnnotation(ConfigProperty.class);
        if (configPropertyAnnotation == null) {
            return field.getName().equals(tag);
        }

        for (String t : configPropertyAnnotation.tags()) {
            if (t.equals(tag)) {
                return true;
            }
        }

        return field.getName().equals(tag);
    }

    @Override
    public Class getOwner() {
        return field.getDeclaringClass();
    }

    private boolean isInstanceOf(Class type, Object value) {
        return Primitives.wrap(type).isInstance(value);
    }

}
