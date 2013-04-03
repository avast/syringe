package com.avast.syringe.config;

import com.avast.syringe.config.internal.InjectableProperty;

/**
 * Allows transforming the value being injected to a property (field).
 * <p/>
 * User: slajchrt
 * Date: 4/3/12
 * Time: 3:11 PM
 */
public interface PropertyValueConverter {

    <T> T convertTo(InjectableProperty property, Object instance, Class<T> targetPropertyClass, Object sourceValue);

}
