package com.avast.syringe.config;

import com.avast.syringe.config.internal.ConfigClassAnalyzer;
import com.avast.syringe.config.internal.InjectableProperty;
import com.avast.syringe.config.internal.ReflectionInjectableProperty;
import com.avast.syringe.config.internal.TypeConversion;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import nu.xom.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Generates an XML schema file describing the XML configuration for a given
 * configuration class. Can be used via a {@link #generateXmlSchema(OutputStream) Java API}
 * and it also provides a {@link #main(String...)}} method.
 */
public class XmlSchemaGenerator {

    public static final String NS_SCHEMA = "http://www.w3.org/2001/XMLSchema";
    public static final String SCHEMAS_URL = "http://www.avast.com/schemas/";

    private final Class<?> configClass;
    private final String namespaceUri;

    public static void main(String... args) throws Exception {

//        Package[] packages = Package.getPackages();
//        for (Package aPackage : packages) {
//            System.out.println(aPackage);
//        }
//
        if (args.length < 1) {
            System.out.println("Usage: java " + XmlSchemaGenerator.class + "<class> [<namespace-uri>]");
            System.exit(1);
        }

        Class<?> configClass = Class.forName(args[0]);
        if (args.length < 2) {
            new XmlSchemaGenerator(configClass).generateXmlSchema(System.out);
        } else {
            String namespaceUri = args[1];
            new XmlSchemaGenerator(configClass, namespaceUri).generateXmlSchema(System.out);
        }
    }

    public XmlSchemaGenerator(Class<?> configClass) {
        this(configClass, XmlInstanceGenerator.getNamespace(configClass));
    }

    public XmlSchemaGenerator(Class<?> configClass, String namespaceUri) {
        Preconditions.checkNotNull(configClass, "configClass");
        Preconditions.checkNotNull(namespaceUri, "namespaceUri");

        this.configClass = configClass;
        this.namespaceUri = namespaceUri;
    }

    public void generateXmlSchema(OutputStream output) throws IOException {
        Element schemaEl = new Element("xs:schema", NS_SCHEMA);
        schemaEl.addNamespaceDeclaration(null, namespaceUri);
        schemaEl.addAttribute(new Attribute("targetNamespace", namespaceUri));
        schemaEl.addAttribute(new Attribute("elementFormDefault", "qualified"));

        Element topElementEl = new Element("xs:element", NS_SCHEMA);
        topElementEl.addAttribute(new Attribute("name", "config"));
        schemaEl.appendChild(topElementEl);

        Element topComplexTypeEl = new Element("xs:complexType", NS_SCHEMA);
        topElementEl.appendChild(topComplexTypeEl);

        final Element topAllEl = new Element("xs:all", NS_SCHEMA);
        topComplexTypeEl.appendChild(topAllEl);

        // 'decorators' attribute in the <config> element
        final Element decorAttrEl = new Element("xs:attribute", NS_SCHEMA);
        decorAttrEl.addAttribute(new Attribute("name", "decorators"));
        decorAttrEl.addAttribute(new Attribute("type", "xs:string"));
        decorAttrEl.addAttribute(new Attribute("use", "optional"));

        topComplexTypeEl.appendChild(decorAttrEl);

        Set<InjectableProperty> listProperties = Sets.newHashSet();
        Set<InjectableProperty> mapProperties = Sets.newHashSet();
        Set<InjectableProperty> refProperties = Sets.newHashSet();

        HashSet<String> declaredRefTypes = Sets.newHashSet();

        for (InjectableProperty property : new ConfigClassAnalyzer(configClass).getConfigProperties()) {

            if (property.isContextual()) {
                // Ignore contextual properties
                continue;
            }

            String propTypeName;
            if (property.isArray() || property.isCollection()) {
                propTypeName = property.getName() + "Type";
                listProperties.add(property);
            } else if (property.isMap()) {
                propTypeName = property.getName() + "Type";
                mapProperties.add(property);
            } else if (property.isReference()) {
                propTypeName = property.getType().getName();
                refProperties.add(property);
            } else {
                propTypeName = TypeConversion.getXmlSchemaTypeName(property.getType());
            }

            Element elementEl = new Element("xs:element", NS_SCHEMA);
            elementEl.addAttribute(new Attribute("name", property.getName()));
            elementEl.addAttribute(new Attribute("type", propTypeName));
            String minOccurs = "1";
            if (property.isOptional()) {
                minOccurs = "0";
            }
            elementEl.addAttribute(new Attribute("minOccurs", minOccurs));
            elementEl.addAttribute(new Attribute("maxOccurs", "1"));
            topAllEl.appendChild(elementEl);
        }

        for (InjectableProperty property : listProperties) {
            Class<?> componentType = property.getArrayOrCollectionComponentType();
            String componentTypeName = TypeConversion.getXmlSchemaTypeName(componentType);

            Element complexTypeEl = new Element("xs:complexType", NS_SCHEMA);
            complexTypeEl.addAttribute(new Attribute("name", property.getName() + "Type"));
            schemaEl.appendChild(complexTypeEl);

            appendTypeInfoAnnotation(complexTypeEl, "list");

            Element sequenceEl = new Element("xs:sequence", NS_SCHEMA);
            complexTypeEl.appendChild(sequenceEl);

            Element elementEl = new Element("xs:element", NS_SCHEMA);
            elementEl.addAttribute(new Attribute("name", "value"));
            elementEl.addAttribute(new Attribute("type", componentTypeName));
            if (property.isOptional()) {
                elementEl.addAttribute(new Attribute("minOccurs", "0"));
            }
            elementEl.addAttribute(new Attribute("maxOccurs", "unbounded"));
            sequenceEl.appendChild(elementEl);

            // Declare the reference type
            if (ReflectionInjectableProperty.isReference(componentType)) {
                declareReferenceType(schemaEl, declaredRefTypes, componentTypeName);
            }
        }

        for (InjectableProperty property : mapProperties) {
            Class<?> keyType = property.getMapKeyType();
            Class<?> valueType = property.getMapValueType();
            String keyTypeName = TypeConversion.getXmlSchemaTypeName(keyType);
            String valueTypeName = TypeConversion.getXmlSchemaTypeName(valueType);

            Element complexTypeEl = new Element("xs:complexType", NS_SCHEMA);
            complexTypeEl.addAttribute(new Attribute("name", property.getName() + "Type"));
            schemaEl.appendChild(complexTypeEl);

            appendTypeInfoAnnotation(complexTypeEl, "map");

            Element sequenceEl = new Element("xs:sequence", NS_SCHEMA);
            complexTypeEl.appendChild(sequenceEl);

            Element elementEl = new Element("xs:element", NS_SCHEMA);
            elementEl.addAttribute(new Attribute("name", "entry"));
            if (property.isOptional()) {
                elementEl.addAttribute(new Attribute("minOccurs", "0"));
            }
            elementEl.addAttribute(new Attribute("maxOccurs", "unbounded"));
            sequenceEl.appendChild(elementEl);

            Element subComplexTypeEl = new Element("xs:complexType", NS_SCHEMA);
            elementEl.appendChild(subComplexTypeEl);

            Element simpleContentEl = new Element("xs:simpleContent", NS_SCHEMA);
            subComplexTypeEl.appendChild(simpleContentEl);

            Element extensionEl = new Element("xs:extension", NS_SCHEMA);
            extensionEl.addAttribute(new Attribute("base", valueTypeName));
            simpleContentEl.appendChild(extensionEl);

            Element attributeEl = new Element("xs:attribute", NS_SCHEMA);
            attributeEl.addAttribute(new Attribute("name", "key"));
            attributeEl.addAttribute(new Attribute("type", keyTypeName));
            attributeEl.addAttribute(new Attribute("use", "required"));
            extensionEl.appendChild(attributeEl);

            // Declare the reference type
            if (ReflectionInjectableProperty.isReference(keyType)) {
                declareReferenceType(schemaEl, declaredRefTypes, keyTypeName);
            }

            // Declare the reference type
            if (ReflectionInjectableProperty.isReference(valueType)) {
                declareReferenceType(schemaEl, declaredRefTypes, valueTypeName);
            }

        }

        for (InjectableProperty property : refProperties) {
            String typeName = property.getType().getName();
            declareReferenceType(schemaEl, declaredRefTypes, typeName);
        }

        Serializer serializer = new Serializer(output, "utf-8");
        serializer.setIndent(4);
        serializer.setMaxLength(160);
        serializer.write(new Document(schemaEl));
    }

    private void declareReferenceType(Element schemaEl, HashSet<String> declaredRefTypes, String typeName) {
        if (declaredRefTypes.contains(typeName)) {
            return;
        }

        Element simpleTypeEl = new Element("xs:simpleType", NS_SCHEMA);
        simpleTypeEl.addAttribute(new Attribute("name", typeName));
        schemaEl.appendChild(simpleTypeEl);

        appendTypeInfoAnnotation(simpleTypeEl, "reference");

        Element restrictionEl = new Element("xs:restriction", NS_SCHEMA);
        simpleTypeEl.appendChild(restrictionEl);

        restrictionEl.addAttribute(new Attribute("base", "xs:string"));

        declaredRefTypes.add(typeName);
    }

    private void appendTypeInfoAnnotation(Element complexTypeEl, String typeInfo) {
        Element annotEl = new Element("xs:annotation", NS_SCHEMA);
        complexTypeEl.appendChild(annotEl);

        Element appInfoEl = new Element("xs:appinfo", NS_SCHEMA);
        annotEl.appendChild(appInfoEl);

        Text appInfoContent = new Text("type_info");
        appInfoEl.appendChild(appInfoContent);

        Element docEl = new Element("xs:documentation", NS_SCHEMA);
        annotEl.appendChild(docEl);

        Text docElContent = new Text(typeInfo);
        docEl.appendChild(docElContent);
    }


}
