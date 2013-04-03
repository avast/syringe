package com.avast.syringe.config.mbean;

import com.avast.syringe.config.internal.InjectableProperty;
import com.google.common.collect.Lists;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * User: slajchrt
 * Date: 3/29/12
 * Time: 4:11 PM
 */
public class ConfigMBeanInfoFactory {

    public static MBeanInfo getInfo(String className, String description, Collection<InjectableProperty> properties) {
        List<MBeanAttributeInfo> attrs = Lists.newArrayList();

        for (InjectableProperty prop : properties) {

            boolean isWritable = prop.isAtomic();

            MBeanAttributeInfo attrInfo;
            if (prop.isReference()) {
                attrInfo = new MBeanAttributeInfo(prop.getName(), String.class.getName(), null,
                        true, isWritable, false);
            } else if (prop.isCollection()) {
                attrInfo = new MBeanAttributeInfo(prop.getName(), Collection.class.getName(), null,
                        true, isWritable, false);
            } else if (prop.isArray()) {
                attrInfo = new MBeanAttributeInfo(prop.getName(), Collection.class.getName(), null,
                        true, isWritable, false);
            } else if (prop.isMap()) {
                attrInfo = new MBeanAttributeInfo(prop.getName(), Map.class.getName(), null,
                        true, isWritable, false);
            } else {
                attrInfo = new MBeanAttributeInfo(prop.getName(), prop.getType().getName(),
                        null, true, isWritable, false);
            }

            attrs.add(attrInfo);
        }

        return new MBeanInfo(className, description, attrs.toArray(new MBeanAttributeInfo[attrs.size()]),
                null, null, null);

    }

}
