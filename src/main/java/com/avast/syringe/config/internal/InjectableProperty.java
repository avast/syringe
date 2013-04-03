package com.avast.syringe.config.internal;

/**
 * Describes a property (broadly speaking: it could be a field or a Java Beans property)
 * that we can inject values into. It does not handle the actual injection,
 * but it provides information about the property, such as its type and the type
 * of its components for an array- or collection- or map-typed property.
 */
public interface InjectableProperty extends GeneratedProperty {

    String getName();

    String getXmlSchemaTypeName();

    boolean isReference();

    boolean isArray();

    boolean isCollection();

    boolean isMap();

    boolean isDelegate();

    boolean isOptional();

    Class<?> getArrayOrCollectionComponentType();

    boolean isContextual();

    Class<?> getType();

    boolean isAtomic();

    Class getAtomicType();

    /**
     * For map-typed properties (i.e., only applies when {@link #isMap()} is {@code true}.
     * @return
     */
    Class<?> getMapKeyType();

    /**
     * For map-typed properties (i.e., only applies when {@link #isMap()} is {@code true}.
     * @return
     */
    Class<?> getMapValueType();

    Object getValue(Object instance) throws Exception;

    void setValue(Object instance, Object value) throws Exception;

    void putMapEntry(Object instance, Object mapKey, Object mapValue) throws Exception;

    void addCollectionElement(Object instance, Object element) throws Exception;

    void setArrayElement(Object instance, int i, Object element) throws Exception;

    /**
     * @return true if the property is tagged with the tag
     * @see com.avast.syringe.config.ConfigProperty#tags()
     */
    boolean hasTag(String tag);

    /**
     * @return the enclosing class
     */
    Class getOwner();
}