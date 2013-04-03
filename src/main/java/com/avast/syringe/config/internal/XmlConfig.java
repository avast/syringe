package com.avast.syringe.config.internal;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents the product of {@link XmlConfigParser}.
 * <p/>
 * User: slajchrt
 * Date: 3/12/12
 * Time: 11:13 AM
 */
public class XmlConfig {

    public static class Decorator {
        private final String name;

        public Decorator(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private final Map<String, Property> propertyMap;
    private final String namespaceURI;
    private final String className;
    private final List<Decorator> decorators = Lists.newArrayList();

    public XmlConfig(Map<String, Property> propertyMap, String namespaceURI, String className,
                     List<Decorator> decorators) {
        this.propertyMap = ImmutableMap.copyOf(propertyMap);
        this.namespaceURI = namespaceURI;
        this.className = className;
        this.decorators.addAll(decorators);
    }

    public String getClassName() {
        return className;
    }

    public Map<String, Property> getProperties() {
        return propertyMap;
    }

    public String getNamespaceURI() {
        return namespaceURI;
    }

    public List<Decorator> getDecorators() {
        return Collections.unmodifiableList(decorators);
    }
}
