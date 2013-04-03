package com.avast.syringe.config.perspective;

import com.avast.syringe.config.internal.InjectableProperty;

/**
 * Provides contextual values for config properties.
 * <p/>
 * User: slajchrt
 * Date: 6/14/12
 * Time: 9:54 AM
 */
public interface PropertyResolver {

    /**
     * @param instance the owner instance of the property
     * @param property the property
     * @return true if the value for the property is available
     */
    boolean hasPropertyValue(Object instance, InjectableProperty property);

    /**
     * @param instance the owner instance of the property
     * @param property the property
     * @return the property value
     * @throws NoSuchFieldException if there is no property value for the given property
     */
    Object getPropertyValue(Object instance, InjectableProperty property) throws NoSuchFieldException;

}
