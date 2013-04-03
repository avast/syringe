package com.avast.syringe.config.internal;

import org.apache.xerces.impl.xs.XSComplexTypeDecl;
import org.apache.xerces.jaxp.validation.XMLSchemaFactory;
import org.apache.xerces.xs.XSAnnotation;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.TypeInfo;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.TypeInfoProvider;
import javax.xml.validation.ValidatorHandler;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: slajchrt
 * Date: 3/12/12
 * Time: 8:42 PM
 */
public class XmlConfigHandlerTest {

    public static final String CONFIG_TEST_2_XSD = "config-test-2.xsd";
    public static final String CONFIG_TEST_2_XML = "config-test-2.xml";
    public static final String CONFIG_TEST_2_DECOR_XML = "config-test-2-decor.xml";

    @Test
    public void testReadTypeInfoAnnotation() throws Exception {
        XSAnnotation annotation = readListTypeAnnotation("list");

        PropertyTypeInfoAnnotation propertyTypeInfoAnnotation = XmlConfigHandler.readTypeInfoAnnotation(annotation);
        Assert.assertEquals("list", propertyTypeInfoAnnotation.getPropertyTypeInfo());
    }

    @Test
    public void testGetValueScopeHandlerType() throws IOException, SAXException {
        TypeInfo typeDecl = readTypeForProperty("list");
        Assert.assertEquals(XmlConfigHandler.ValueScopeHandlerType.LIST, XmlConfigHandler.getValueScopeHandlerType(typeDecl));

        typeDecl = readTypeForProperty("map");
        Assert.assertEquals(XmlConfigHandler.ValueScopeHandlerType.MAP, XmlConfigHandler.getValueScopeHandlerType(typeDecl));

        typeDecl = readTypeForProperty("port");
        Assert.assertEquals(XmlConfigHandler.ValueScopeHandlerType.SCALAR, XmlConfigHandler.getValueScopeHandlerType(typeDecl));

        typeDecl = readTypeForProperty("ref");
        Assert.assertEquals(XmlConfigHandler.ValueScopeHandlerType.REF, XmlConfigHandler.getValueScopeHandlerType(typeDecl));
    }

    @Test
    public void testParse() throws Exception {
        String xmlInstance = CONFIG_TEST_2_XML;
        parseAndAssertInstance(xmlInstance);
    }

    @Test
    public void testParseDecor() throws Exception {
        String xmlInstance = CONFIG_TEST_2_DECOR_XML;
        XmlConfigHandler xmlConfigHandler = parseAndAssertInstance(xmlInstance);

        List<XmlConfig.Decorator> decorators = xmlConfigHandler.getDecorators();
        Assert.assertEquals(2, decorators.size());
        Assert.assertEquals("Decor1", decorators.get(0).getName());
        Assert.assertEquals("Decor2", decorators.get(1).getName());
    }

    private XmlConfigHandler parseAndAssertInstance(String xmlInstance) throws SAXException, IOException {
        InputStream schemaStream = XmlConfigHandlerTest.class.getResourceAsStream(CONFIG_TEST_2_XSD);

        XMLSchemaFactory xmlSchemaFactory = new XMLSchemaFactory();
        Schema schema = xmlSchemaFactory.newSchema(new StreamSource(schemaStream));

        ValidatorHandler validatorHandler = schema.newValidatorHandler();
        final TypeInfoProvider typeInfoProvider = validatorHandler.getTypeInfoProvider();
        XmlConfigHandler xmlConfigHandler = new XmlConfigHandler(typeInfoProvider);
        validatorHandler.setContentHandler(xmlConfigHandler);

        XMLReader parser = XMLReaderFactory.createXMLReader();
        parser.setContentHandler(validatorHandler);
        InputStream xmlStream = XmlConfigHandlerTest.class.getResourceAsStream(xmlInstance);
        parser.parse(new InputSource(xmlStream));

        assertInstance(validatorHandler, xmlConfigHandler, xmlInstance);
        return xmlConfigHandler;
    }

    private void assertInstance(ValidatorHandler validatorHandler, XmlConfigHandler xmlConfigHandler, String xmlInstance) throws SAXException, IOException {

        Map<String,Property> properties = xmlConfigHandler.getProperties();
        Assert.assertEquals(4, properties.size());
        Assert.assertEquals("port", properties.get("port").getName());
        Assert.assertEquals("100", properties.get("port").getValues().get(0).getValue());
        Assert.assertEquals("ref", properties.get("ref").getName());
        Assert.assertEquals("FileRepHandler", properties.get("ref").getValues().get(0).getValue());
        Assert.assertEquals("com.avast.cloud.RequestHandler", (properties.get("ref").getValues().get(0)).getRefType());
        Assert.assertEquals("list", properties.get("list").getName());
        Assert.assertEquals("aaa", properties.get("list").getValues().get(0).getValue());
        Assert.assertEquals("bbb", properties.get("list").getValues().get(1).getValue());
        Assert.assertEquals("ccc", properties.get("list").getValues().get(2).getValue());
        Assert.assertEquals("map", properties.get("map").getName());
        Assert.assertEquals("a", ((MapEntry)properties.get("map").getValues().get(0)).getKey());
        Assert.assertEquals("1", properties.get("map").getValues().get(0).getValue());
        Assert.assertEquals("b", ((MapEntry)properties.get("map").getValues().get(1)).getKey());
        Assert.assertEquals("2", properties.get("map").getValues().get(1).getValue());
        Assert.assertEquals("c", ((MapEntry)properties.get("map").getValues().get(2)).getKey());
        Assert.assertEquals("3", properties.get("map").getValues().get(2).getValue());
    }


    private XSAnnotation readListTypeAnnotation(final String propertyName) throws SAXException, IOException {
        XSComplexTypeDecl typeInfo = (XSComplexTypeDecl) readTypeForProperty(propertyName);
        return (XSAnnotation)typeInfo.getAnnotations().item(0);
    }

    private TypeInfo readTypeForProperty(final String propertyName) throws SAXException, IOException {
        InputStream schemaStream = XmlConfigHandlerTest.class.getResourceAsStream(CONFIG_TEST_2_XSD);

        XMLSchemaFactory xmlSchemaFactory = new XMLSchemaFactory();
        Schema schema = xmlSchemaFactory.newSchema(new StreamSource(schemaStream));

        final AtomicReference<TypeInfo> propertyTypeInfo = new AtomicReference<TypeInfo>();

        ValidatorHandler validatorHandler = schema.newValidatorHandler();
        final TypeInfoProvider typeInfoProvider = validatorHandler.getTypeInfoProvider();
        validatorHandler.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                if (propertyName.equals(localName)) {
                    propertyTypeInfo.set(typeInfoProvider.getElementTypeInfo());
                }
            }
        });

        XMLReader parser = XMLReaderFactory.createXMLReader();
        parser.setContentHandler(validatorHandler);
        InputStream xmlStream = XmlConfigHandlerTest.class.getResourceAsStream(CONFIG_TEST_2_XML);
        parser.parse(new InputSource(xmlStream));

        return propertyTypeInfo.get();
    }

}
