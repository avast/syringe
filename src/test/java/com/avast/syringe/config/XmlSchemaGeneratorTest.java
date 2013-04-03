package com.avast.syringe.config;

import org.apache.xerces.jaxp.validation.XMLSchemaFactory;
import org.junit.Test;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;

/**
 * User: slajchrt
 * Date: 3/13/12
 * Time: 5:04 PM
 */
public class XmlSchemaGeneratorTest {

    @Test
    public void testGenerateXmlSchema() throws Exception {
        XmlSchemaGenerator gen = new XmlSchemaGenerator(SampleA.class);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        gen.generateXmlSchema(bos);

        String output = new String(bos.toByteArray());

        XMLSchemaFactory sf = new XMLSchemaFactory(); // Xerces
        Schema schema = sf.newSchema(new StreamSource(new StringReader(output)));
        Validator validator = schema.newValidator();

        InputStream xmlStream = XmlSchemaGeneratorTest.class.getResourceAsStream("SampleA.xml");
        validator.validate(new StreamSource(xmlStream));

    }


    public static void main(String[] args) throws Exception {
        XmlSchemaGenerator.main("com.avast.syringe.config.SampleC");
    }
}
