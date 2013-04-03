package com.avast.syringe.config;

import com.avast.syringe.config.internal.ConfigClassAnalyzer;
import com.avast.syringe.config.internal.InjectableProperty;
import com.avast.syringe.config.internal.TypeConversion;
import com.google.common.base.Preconditions;
import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;

import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;

/**
 * Generates an XML instance file describing the XML configuration for a given
 * configuration class.
 */
public class XmlInstanceGenerator {

    public static final String SCHEMAS_URL = "http://www.avast.com/schemas/";

    private final Class<?> configClass;
    private final Object configInstance;
    private final String namespaceUri;

    public static void main(String... args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java " + XmlInstanceGenerator.class + "<class> <excludeOptional> [<namespace-uri>]");
            System.exit(1);
        }

        Class<?> configClass = Class.forName(args[0]);
        if (args.length < 3) {
            new XmlInstanceGenerator(configClass).generateXmlSchema(System.out, Boolean.parseBoolean(args[1]));
        } else {
            String namespaceUri = args[2];
            new XmlInstanceGenerator(configClass, namespaceUri).generateXmlSchema(System.out,
                    Boolean.parseBoolean(args[1]));
        }
    }

    public XmlInstanceGenerator(Class<?> configClass) throws Exception {
        this(configClass, getNamespace(configClass));
    }

    public static String getNamespace(Class<?> configClass) {
        return SCHEMAS_URL + configClass.getName().replace('.', '/');
    }

    public XmlInstanceGenerator(Class<?> configClass, String namespaceUri) throws Exception {
        Preconditions.checkNotNull(configClass, "configClass");
        Preconditions.checkNotNull(namespaceUri, "namespaceUri");

        this.configClass = configClass;
        this.configInstance = configClass.newInstance();
        this.namespaceUri = namespaceUri;
    }

    public void generateXmlSchema(OutputStream output, boolean excludeOptional) throws Exception {
        Element configEl = new Element("config", namespaceUri);
        configEl.addNamespaceDeclaration(null, namespaceUri);

        for (InjectableProperty property : new ConfigClassAnalyzer(configClass).getConfigProperties()) {
            if (excludeOptional && property.isOptional()) {
                continue;
            }

            if (property.isContextual()) {
                continue;
            }

            if (property.isArray()) {
                addArrayProperty(configEl, property);
            } else if (property.isCollection()) {
                addCollectionProperty(configEl, property);
            } else if (property.isMap()) {
                addMapProperty(configEl, property);
            } else if (property.isReference()) {
                addReferenceProperty(configEl, property);
            } else {
                addScalarProperty(configEl, property);
            }

        }

        Serializer serializer = new Serializer(output, "utf-8");
        serializer.setIndent(4);
        serializer.setMaxLength(160);
        serializer.write(new Document(configEl));
    }

    private void addScalarProperty(Element configEl, InjectableProperty property) throws Exception {
        Element valueEl = new Element(property.getName(), namespaceUri);
        configEl.appendChild(valueEl);

        Object value = property.getValue(configInstance);
        if (value != null) {
            String stringValue = TypeConversion.convertToString(value, (Class<Object>) property.getType());
            valueEl.appendChild(stringValue);
        }
    }

    private void addReferenceProperty(Element configEl, InjectableProperty property) {
        Element valueEl = new Element(property.getName(), namespaceUri);
        configEl.appendChild(valueEl);

        String refName;
        if (!property.getType().isInterface() && !Modifier.isAbstract(property.getType().getModifiers())) {
            refName = property.getType().getSimpleName();
        } else {
            refName = "TODO";
        }

        valueEl.appendChild(refName);
    }

    private void addArrayProperty(Element configEl, InjectableProperty property) throws Exception {
        Element valueEl = new Element(property.getName(), namespaceUri);
        configEl.appendChild(valueEl);

        Object value = property.getValue(configInstance);
        if (value != null) {

            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                Element elementEl = new Element("value", namespaceUri);
                valueEl.appendChild(elementEl);

                Object elementValue = Array.get(value, i);
                String stringValue = TypeConversion.convertToString(elementValue, (Class<Object>) property.getType());
                elementEl.appendChild(stringValue);
            }

        } else {
            Element elementEl = new Element("value", namespaceUri);
            valueEl.appendChild(elementEl);
        }
    }

    private void addCollectionProperty(Element configEl, InjectableProperty property) throws Exception {
        Element valueEl = new Element(property.getName(), namespaceUri);
        configEl.appendChild(valueEl);

        Object value = property.getValue(configInstance);
        if (value != null) {

            Collection collection = (Collection) value;
            for (Object elementValue : collection) {
                Element elementEl = new Element("value", namespaceUri);
                valueEl.appendChild(elementEl);

                String stringValue = TypeConversion.convertToString(elementValue,
                        (Class<Object>) property.getArrayOrCollectionComponentType());
                elementEl.appendChild(stringValue);
            }
        } else {
            Element elementEl = new Element("value", namespaceUri);
            valueEl.appendChild(elementEl);
        }
    }

    private void addMapProperty(Element configEl, InjectableProperty property) throws Exception {
        Element valueEl = new Element(property.getName(), namespaceUri);
        configEl.appendChild(valueEl);

        Object value = property.getValue(configInstance);
        if (value != null) {

            Map<?, ?> map = (Map<?, ?>) value;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Element elementEl = new Element("entry", namespaceUri);
                valueEl.appendChild(elementEl);

                String stringValue = TypeConversion.convertToString(entry.getValue(),
                        (Class<Object>) property.getMapValueType());
                elementEl.appendChild(stringValue);

                String keyStringValue = TypeConversion.convertToString(entry.getKey(),
                        (Class<Object>) property.getMapKeyType());
                elementEl.addAttribute(new Attribute("key", keyStringValue));
            }
        } else {
            Element elementEl = new Element("entry", namespaceUri);
            elementEl.addAttribute(new Attribute("key", ""));
            valueEl.appendChild(elementEl);
        }
    }

}
