package com.avast.syringe.config.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class XmlConfigParserTest {

    public static final String SAMPLE_XML = "../SampleA.xml";

    @Test
    public void testLoadDocument() throws Exception {
        String xml =
                "<config-test xmlns='http://www.avast.com/cloud/myavast' xmlns:c='http://www.avast.com/cloud/config'>" +
                "   <port>8080</port>" +
                "   <list><value>1</value><value>2</value></list>" +
                "   <map><entry key='key'>42</entry></map>" +
                "</config-test>";
        Map<String, Property> props = parse(xml);

        Assert.assertEquals(3, props.size());

        Assert.assertEquals("8080", props.get("port").getValues().get(0).getValue());

        Property list = props.get("list");
        Assert.assertEquals(2, list.getValues().size());
        Assert.assertEquals(ImmutableList.of(new Value("1"), new Value("2")), list.getValues());

        Property map = props.get("map");
        Assert.assertEquals(1, map.getValues().size());
        Assert.assertEquals(ImmutableList.of(new MapEntry("key", "42")), map.getValues());
    }

    @Test
    public void testLoadConfig() throws Exception {
        InputStream xmlStream = XmlConfigHandlerTest.class.getResourceAsStream(XmlConfigHandlerTest.CONFIG_TEST_2_XML);
        XmlConfig xmlConfig = new XmlConfigParser().loadConfig(xmlStream);
        Assert.assertEquals("http://www.avast.com/schemas/com/avast/myapp/MyAppRepRequestHandler", xmlConfig.getNamespaceURI());
        Assert.assertEquals("com.avast.myapp.MyAppRepRequestHandler", xmlConfig.getClassName());
        Assert.assertEquals(4, xmlConfig.getProperties().size());
    }

    @Test
    public void testLoadConfigMoreComplex() throws Exception {
        InputStream xmlStream = XmlConfigHandlerTest.class.getResourceAsStream(SAMPLE_XML);
        XmlConfig xmlConfig = new XmlConfigParser().loadConfig(xmlStream);
        Assert.assertEquals("http://www.avast.com/schemas/com/avast/syringe/config/SampleA", xmlConfig.getNamespaceURI());
        Assert.assertEquals("com.avast.syringe.config.SampleA", xmlConfig.getClassName());
        Assert.assertEquals(9, xmlConfig.getProperties().size());
    }

    private Map<String, Property> parse(String xml) throws Exception {
        return new XmlConfigParser(XmlConfigParserTest.class.getResource("config-test.xsd")).loadProperties(toInputStream(xml));
    }

    private InputStream toInputStream(String xml) throws IOException {
        return new ByteArrayInputStream(xml.getBytes("utf-8"));
    }
}
