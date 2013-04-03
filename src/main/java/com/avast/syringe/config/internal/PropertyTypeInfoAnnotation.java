package com.avast.syringe.config.internal;

/**
 * User: slajchrt
 * Date: 3/12/12
 * Time: 6:49 PM
 */
public class PropertyTypeInfoAnnotation {
    private String typeInfo;

    public PropertyTypeInfoAnnotation(String typeInfo) {
        this.typeInfo = typeInfo;
    }

    public String getPropertyTypeInfo() {
        return typeInfo;
    }
}
