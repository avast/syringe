package com.avast.syringe.config.mbean;

import com.avast.syringe.config.InstanceManager;
import com.avast.syringe.config.internal.InjectableProperty;
import com.avast.syringe.config.internal.Injection;
import com.avast.syringe.config.internal.ReflectionInjectableProperty;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.util.*;

/**
 * User: slajchrt
 * Date: 3/29/12
 * Time: 4:29 PM
 */
public class ConfigDynamicBean implements DynamicMBean {

    private static Logger LOGGER = LoggerFactory.getLogger(ConfigDynamicBean.class);

    private final MBeanInfo mBeanInfo;
    private final Object instance;
    private final Map<String, InjectableProperty> propertyMap = Maps.newHashMap();
    private final InstanceManager loader;

    public ConfigDynamicBean(Object instance, String className, String description, List<Injection> injections,
                             InstanceManager loader) {
        this(instance, className, description, Lists.transform(injections, new Function<Injection, InjectableProperty>() {
            @Override
            public InjectableProperty apply(Injection input) {
                return input.getProperty();
            }
        }), loader);
    }

    public ConfigDynamicBean(Object instance, String className, String description,
                             Collection<InjectableProperty> properties, InstanceManager loader) {
        this.mBeanInfo = ConfigMBeanInfoFactory.getInfo(className, description, properties);
        this.instance = instance;
        this.loader = loader;

        for (InjectableProperty property : properties) {
            propertyMap.put(property.getName(), property);
        }
    }

    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
        try {
            InjectableProperty prop = propertyMap.get(attribute);

            if (prop.isArray()) {
                // todo - will return a collection instead of an array
                return null;
            } else if (prop.isCollection()) {
                Collection collection = (Collection) prop.getValue(instance);
                if (ReflectionInjectableProperty.isReference(prop.getArrayOrCollectionComponentType())) {
                    return getRefNames(collection);
                } else {
                    return collection;
                }
            } else if (prop.isMap()) {
                Map map = (Map) prop.getValue(instance);
                return getRefNames(map, prop);
            } else if (prop.isReference()) {
                Object ref = prop.getValue(instance);
                return loader.findRefName(ref);
            } else {
                return prop.getValue(instance);
            }
        } catch (Exception e) {
            LOGGER.error("Cannot get attribute", e);
            throw new IllegalArgumentException(e.getMessage());
        }

    }

    private Object getRefNames(Collection collection) {
        List<String> attrValue = new ArrayList<String>();
        for (Object value : collection) {
            attrValue.add(loader.findRefName(value));
        }
        return attrValue;
    }

    private Object getRefNames(Map<?, ?> map, InjectableProperty mapProp) {
        Map attrValue = new TreeMap();
        for (Map.Entry<?, ?> entry : map.entrySet()) {

            Object k;
            if (ReflectionInjectableProperty.isReference(mapProp.getMapKeyType())) {
                k = loader.findRefName(entry.getKey());
            } else {
                k  = entry.getKey();
            }

            Object v;
            if (ReflectionInjectableProperty.isReference(mapProp.getMapValueType())) {
                v = loader.findRefName(entry.getValue());
            } else {
                v  = entry.getKey();
            }

            attrValue.put(k, v);
        }
        return attrValue;
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException,
            MBeanException, ReflectionException {
        InjectableProperty prop = propertyMap.get(attribute.getName());

        if (prop.isReference()) {
            setReference(attribute, prop);
        } else {
            try {
                prop.setValue(instance, attribute.getValue());
            } catch (Exception e) {
                LOGGER.error("Cannot set attribute", e);
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    private void setReference(Attribute attribute, InjectableProperty prop)  {
        String refName = (String)attribute.getValue();
        Object refInst;
        if (refName != null) {
            try {
                refInst = loader.load(refName);
            } catch (Exception e) {
                throw new IllegalArgumentException("Unable to load referenced instance: " + e.getMessage());
            }
            if (refInst == null) {
                throw new IllegalArgumentException("Invalid reference name: " + refName);
            }
        } else {
            refInst = null;
        }

        try {
            prop.setValue(instance, refInst);
        } catch (Exception e) {
            LOGGER.error("Cannot read attribute", e);
            throw new IllegalArgumentException(e.getMessage());
        }

    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
        try {
            AttributeList attributeList = new AttributeList();
            for (String attrName : attributes) {
                attributeList.add(new Attribute(attrName, getAttribute(attrName)));
            }

            return attributeList;
        } catch (Exception e) {
            LOGGER.error("Cannot get attributes", e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        try {
            List<String> attrNames = Lists.newArrayList();
            for (Object a : attributes) {
                Attribute attr = (Attribute)a;
                setAttribute(attr);
                attrNames.add(attr.getName());
            }

            String[] attrNamesArr = attrNames.toArray(new String[attrNames.size()]);
            return getAttributes(attrNamesArr);
        } catch (Exception e) {
            LOGGER.error("Cannot set attributes", e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
        return null;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return mBeanInfo;
    }
}
