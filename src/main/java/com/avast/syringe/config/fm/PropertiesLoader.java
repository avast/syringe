package com.avast.syringe.config.fm;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Loads properties from a file and creates a FreeMarker map object usable for template processing.
 * <p/>
 * User: slajchrt
 * Date: 4/11/12
 * Time: 12:46 PM
 */
public class PropertiesLoader {

    public static Map load(Properties properties) {
        Map map = Maps.newHashMap();
        for (Map.Entry<Object, Object> property : properties.entrySet()) {
            parseProperty(property, map);
        }
        return map;
    }

    private static void parseProperty(Map.Entry property, Map map) {
        String key = (String) property.getKey();
        Preconditions.checkArgument(!key.contains("."), "Avoid using dots in property %s", key);

        Object value;
        if (key.endsWith("]")) {
            int i = key.indexOf('[');
            Preconditions.checkArgument(i > 0, "Invalid index specification in property %s", key);

            String indexAsStr = key.substring(i + 1, key.length() - 1);
            int index = Integer.parseInt(indexAsStr);

            key = key.substring(0, i);
            Object[] propVal = (Object[]) map.get(key);

            propVal = ensureArraySize(index, propVal);

            Preconditions.checkArgument(propVal[index] == null, "Duplicate index %s in property %s", index, key);
            propVal[index] = property.getValue();
            value = propVal;

        } else {
            value = property.getValue();
        }

        map.put(key, value);
    }

    static Object[] ensureArraySize(int index, Object[] propVal) {
        if (propVal == null) {
            propVal = new Object[index + 1];
        } else {
            if (index >= propVal.length) {
                Object[] newVal = new Object[index + 1];
                System.arraycopy(propVal, 0, newVal, 0, propVal.length);
                propVal = newVal;
            }
        }
        return propVal;
    }

}
