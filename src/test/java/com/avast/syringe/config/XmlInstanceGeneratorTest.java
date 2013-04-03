package com.avast.syringe.config;

import junit.framework.Assert;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * User: slajchrt
 * Date: 4/4/12
 * Time: 12:44 PM
 */
public class XmlInstanceGeneratorTest {

    @Test
    public void testGeneration() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        //new XmlInstanceGenerator(SampleA.class).generateXmlSchema(System.out, false);
        new XmlInstanceGenerator(SampleA.class).generateXmlSchema(bos, false);

        Builder domBuilder = new Builder();
        Document doc = domBuilder.build(new ByteArrayInputStream(bos.toByteArray()));

        String namespace = XmlInstanceGenerator.getNamespace(SampleA.class);

        Element config = doc.getRootElement();
        Assert.assertEquals("config", config.getLocalName());

        Elements i = config.getChildElements("i", namespace);
        Assert.assertEquals(1, i.size());
        Assert.assertEquals("0", i.get(0).getValue());

        Elements s = config.getChildElements("s", namespace);
        Assert.assertEquals(1, s.size());
        Assert.assertEquals("abc", s.get(0).getValue());

        Elements r = config.getChildElements("r1", namespace);
        Assert.assertEquals(1, r.size());
        Assert.assertEquals("TODO", r.get(0).getValue());

        r = config.getChildElements("r2", namespace);
        Assert.assertEquals(1, r.size());
        Assert.assertEquals("TODO", r.get(0).getValue());

        r = config.getChildElements("r3", namespace);
        Assert.assertEquals(1, r.size());
        Assert.assertEquals("SampleC", r.get(0).getValue());

        Elements l = config.getChildElements("l", namespace);
        Assert.assertEquals(1, l.size());
        Elements values = l.get(0).getChildElements("value", namespace);
        Assert.assertEquals(2, values.size());
        Assert.assertEquals("abc", values.get(0).getValue());
        Assert.assertEquals("xyz", values.get(1).getValue());

        Elements m = config.getChildElements("m", namespace);
        Assert.assertEquals(1, m.size());
        Elements entries = m.get(0).getChildElements("entry", namespace);
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals("ABC", entries.get(0).getValue());
        Assert.assertEquals("1", entries.get(0).getAttribute("key").getValue());
        Assert.assertEquals("XYZ", entries.get(1).getValue());
        Assert.assertEquals("2", entries.get(1).getAttribute("key").getValue());

        Elements lr = config.getChildElements("lr", namespace);
        Assert.assertEquals(1, lr.size());
        values = lr.get(0).getChildElements("value", namespace);
        Assert.assertEquals(1, values.size());
        Assert.assertEquals("", values.get(0).getValue());

        Elements mr = config.getChildElements("mr", namespace);
        Assert.assertEquals(1, mr.size());
        entries = mr.get(0).getChildElements("entry", namespace);
        Assert.assertEquals(1, entries.size());
        Assert.assertEquals("", entries.get(0).getValue());
        Assert.assertEquals("", entries.get(0).getAttribute("key").getValue());

    }

}
