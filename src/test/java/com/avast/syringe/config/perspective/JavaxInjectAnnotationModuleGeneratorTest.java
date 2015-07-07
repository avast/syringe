/*
 * Copyright (c) 2015 Avast a.s., www.avast.com
 */
package com.avast.syringe.config.perspective;

import com.google.common.io.CharStreams;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Properties;

/**
 * A test whether @Named and @Inject annotations from javax.inject package
 * cause proper generation of module.
 *
 * @author Jan Lastovicka (lastovicka@avast.com)
 * @since 2015-07-01
 */
public class JavaxInjectAnnotationModuleGeneratorTest {

    @Test
    public void test() throws Exception {
        StringWriter writer = new StringWriter();
        Properties builderTraitMappings = new Properties();
        builderTraitMappings.setProperty(".*", "com.avast.BuilderTrait1 \n com.avast.BuilderTrait2");
        ModuleGenerator.getInstance().generate(
                "com.avast.syringe.config.perspective",
                "Description",
                "OneComponentModule",
                Arrays.asList(new Class[]{SampleB.class}),
                Arrays.asList("com.avast.Trait1", "com.avast.Trait2"),
                builderTraitMappings,
                writer);

        String output = writer.toString().trim();

        InputStream expectedOutputStream = ModuleGeneratorTest.class.getResourceAsStream("output2.txt");
        String expectedOutput = CharStreams.toString(new InputStreamReader(expectedOutputStream)).trim();

        Assert.assertEquals(expectedOutput, output);
    }
}
