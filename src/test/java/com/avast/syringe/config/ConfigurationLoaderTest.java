package com.avast.syringe.config;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import com.avast.syringe.config.internal.TestConfigWithContextProperty;
import com.google.common.io.ByteStreams;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.avast.syringe.config.internal.TestConfig;
import com.google.common.io.Files;

public class ConfigurationLoaderTest {
    
    private static File tempDir;
    
    @BeforeClass
    public static void setUp() {
        tempDir = Files.createTempDir();
    }
    
    @AfterClass
    public static void tearDown() throws Exception {
        // Delete it even if it's a symlink.
//        File toDelete = tempDir.getCanonicalFile();
//        Files.deleteRecursively(toDelete);
    }

    @Test
    public void testBasic() throws Exception {
        String xml =
                "<config-test xmlns='http://www.avast.com/cloud/myavast' " +
                        "xmlns:c='http://www.avast.com/cloud/config' " +
                        ">" +
                "   <port>8080</port>" +
                "   <list><value>1</value><value>2</value></list>" +
                "   <map><entry key='key'>42</entry></map>" +
                "</config-test>";
        TestConfig config = load(xml, TestConfig.class);
        
        Assert.assertEquals(8080, config.getPort());
        
        List<Integer> list = config.getList();
        Assert.assertEquals(2, list.size());
        Assert.assertEquals(1, list.get(0).intValue());
        Assert.assertEquals(2, list.get(1).intValue());
        
        Map<String, Long> map = config.getMap();
        Assert.assertEquals(1, map.size());
        Assert.assertEquals(42, map.get("key").longValue());
    }

    private <T> T load(String xml, Class<T> beanClass) throws Exception {
        File file = new File(tempDir, "config.xml");
        Files.write(xml, file, Charset.forName("utf-8"));
        return ConfigurationLoader.load(file, beanClass);
    }


    @Test
    public void testInjectingConfigurationLoader() throws Exception {

        String xml =
                "<config-test xmlns='http://www.avast.com/cloud/myavast' xmlns:c='http://www.avast.com/cloud/config'>" +
                        "   <port>8080</port>" +
                        "   <list><value>1</value><value>2</value></list>" +
                        "   <map><entry key='key'>42</entry></map>" +
                        "</config-test>";

        TestConfigWithContextProperty config = load(xml, TestConfigWithContextProperty.class);
        ConfigurationLoader configLoader = config.getConfigLoader();
        // Assert presence of the context property - the configuration loader
        Assert.assertNotNull(configLoader);

        // Assert values in the inherited properties

        Assert.assertEquals(8080, config.getPort());

        List<Integer> list = config.getList();
        Assert.assertEquals(2, list.size());
        Assert.assertEquals(1, list.get(0).intValue());
        Assert.assertEquals(2, list.get(1).intValue());

        Map<String, Long> map = config.getMap();
        Assert.assertEquals(1, map.size());
        Assert.assertEquals(42, map.get("key").longValue());

    }

    @Test
    public void testWired() throws Exception {
        copyFileToTemp("SampleA.xml");
        copyFileToTemp("SampleB.xml");
        copyFileToTemp("SampleC.xml");

        ConfigurationLoader configLoader = new ConfigurationLoader(tempDir);
        SampleB b = configLoader.load("SampleB.xml");
        Assert.assertEquals("xyz", b.getX());
        SampleA sa = b.getSa();
        Assert.assertNotNull(sa);
        Assert.assertNotNull(sa.getR1());
        Assert.assertNotNull(sa.getR2());
        Assert.assertSame(sa.getR2(), sa.getR1());

    }

    @Test
    public void testOneDecorator() throws Exception {
        copyFileToTemp("SampleC-decor.xml");
        copyFileToTemp("SampleD.xml");

        ConfigurationLoader configLoader = new ConfigurationLoader(tempDir);
        configLoader.setRegisterMBeans(false);

        Runnable c = configLoader.load("SampleC-decor.xml");
        Assert.assertTrue(c instanceof Decorator1);
        Decorator1 d = (Decorator1) c;
        Assert.assertNotNull(d.getDelegate());
        Assert.assertTrue(d.getDelegate() instanceof SampleC);
    }

    @Test
    public void testTwoDecorators() throws Exception {
        copyFileToTemp("SampleC-decor2.xml");
        copyFileToTemp("SampleD.xml");
        copyFileToTemp("SampleE.xml");

        ConfigurationLoader configLoader = new ConfigurationLoader(tempDir);
        configLoader.setRegisterMBeans(false);

        Runnable c = configLoader.load("SampleC-decor2.xml");
        Assert.assertTrue(c instanceof Decorator1);
        Decorator1 d1 = (Decorator1) c;
        Assert.assertNotNull(d1.getDelegate());
        Assert.assertTrue(d1.getDelegate() instanceof Decorator1);

        Decorator1 d2 = (Decorator1) d1.getDelegate();
        Assert.assertNotNull(d2.getDelegate());
        Assert.assertTrue(d2.getDelegate() instanceof SampleC);
    }

    private void copyFileToTemp(String fileName) throws IOException {
        InputStream inputStream = ConfigurationLoaderTest.class.getResourceAsStream(fileName);
        File file = new File(tempDir, fileName);
        FileOutputStream outputStream = new FileOutputStream(file);
        try {
            ByteStreams.copy(inputStream, outputStream);
        } finally {
            inputStream.close();
            outputStream.close();
        }
    }
}
