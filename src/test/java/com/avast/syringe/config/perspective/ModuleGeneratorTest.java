package com.avast.syringe.config.perspective;

import com.google.common.io.CharStreams;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * User: slajchrt
 * Date: 6/4/12
 * Time: 6:14 PM
 */
public class ModuleGeneratorTest {

    @Test
    public void testGenerateOneComponentModule() throws Exception {
        StringWriter writer = new StringWriter();
        Properties builderTraitMappings = new Properties();
        builderTraitMappings.setProperty(".*", "com.avast.BuilderTrait1 \n com.avast.BuilderTrait2");
        ModuleGenerator.getInstance().generate(
                "com.avast.syringe.config.perspective",
                "Description",
                "OneComponentModule",
                Arrays.asList(new Class[]{SampleA.class, SampleProviderA.class, SampleProviderB.class}),
                Arrays.asList("com.avast.Trait1", "com.avast.Trait2"),
                builderTraitMappings,
                writer);

//        System.out.println(writer.toString());

        String output = writer.toString().trim();

        InputStream expectedOutputStream = ModuleGeneratorTest.class.getResourceAsStream("output1.txt");
        String expectedOutput = CharStreams.toString(new InputStreamReader(expectedOutputStream)).trim();

        Assert.assertEquals(expectedOutput, output);
    }

    public static class StringProvider implements com.avast.syringe.Provider<String> {
        @Override
        public String getInstance() {
            return null;
        }
    }

    public static class StringProvider2 extends StringProvider {
    }

    public static class ListOfStringsProvider implements com.avast.syringe.Provider<List<String>> {
        @Override
        public List<String> getInstance() {
            return null;
        }
    }

    public static class ListOfStringsProvider2 extends ListOfStringsProvider {
    }

    public interface MapProvider<K, V> extends com.avast.syringe.Provider<Map<K, V>> {

    }

    public abstract static class GenericProvider<K, V> implements MapProvider<K, V> {
        @Override
        public Map<K, V> getInstance() throws Exception {
            return null;
        }
    }


    public static class SubGenericProvider extends GenericProvider<String, Integer> {

    }

    @Test
    public void testFindProviderParameter() {
        Type providerParameter = ModuleGenerator.findProviderParameter(StringProvider.class);
        Assert.assertEquals(String.class, ModuleGenerator.getClassForType(providerParameter));

        providerParameter = ModuleGenerator.findProviderParameter(StringProvider2.class);
        Assert.assertEquals(String.class, ModuleGenerator.getClassForType(providerParameter));

        providerParameter = ModuleGenerator.findProviderParameter(ListOfStringsProvider.class);
        Assert.assertEquals(List.class, ModuleGenerator.getClassForType(providerParameter));

        providerParameter = ModuleGenerator.findProviderParameter(ListOfStringsProvider2.class);
        Assert.assertEquals(List.class, ModuleGenerator.getClassForType(providerParameter));

        providerParameter = ModuleGenerator.findProviderParameter(SubGenericProvider.class);
        Assert.assertEquals(Map.class, ModuleGenerator.getClassForType(providerParameter));

    }

}
