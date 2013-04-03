package com.avast.syringe.config.internal;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Test;

import com.avast.syringe.config.ConfigException;
import com.avast.syringe.config.ConfigProperty;
import com.google.common.collect.Maps;

public class ConfigInjectorTest {

    @Test
    public void testBasic() throws Exception {
        ConfigInjector<SingleProperty> injector = ConfigInjector.forClass(SingleProperty.class);

        Map<String, Property> props = props(new Property("port", new Value("8080")));
        SingleProperty config = injector.inject(props);

        Assert.assertEquals(8080, config.port);
    }

    @Test
    public void testSubclassing() throws Exception {
        ConfigInjector<TwoPropertiesSubclass> injector = ConfigInjector.forClass(TwoPropertiesSubclass.class);

        Map<String, Property> props = props(new Property("port", new Value("8080")), new Property("host", new Value("localhost")));
        TwoPropertiesSubclass config = injector.inject(props);

        Assert.assertEquals(8080, config.port);
        Assert.assertEquals("localhost", config.host);
    }

    @Test
    public void testRequiredProperty() throws Exception {
        ConfigInjector<TwoProperties> injector = ConfigInjector.forClass(TwoProperties.class);

        Map<String, Property> props = props(new Property("port", new Value("8080")));
        try {
            injector.inject(props);
            Assert.fail();
        } catch (ConfigException e) {
            Assert.assertTrue(e.getCause() instanceof NoSuchFieldException);
        }
    }

    @Test
    public void testOptionalProperty() throws Exception {
        ConfigInjector<OptionalProperty> injector = ConfigInjector.forClass(OptionalProperty.class);

        Map<String, Property> props = props(new Property("host", new Value("localhost")));

        OptionalProperty inject = injector.inject(props);
        Assert.assertEquals("localhost", inject.host);
        Assert.assertEquals(1234, inject.port);
    }

    @Test
    public void testNotParamPropertyNotInjected() throws Exception {
        ConfigInjector<NotParamProperty> injector = ConfigInjector.forClass(NotParamProperty.class);

        Map<String, Property> props = props(new Property("host", new Value("localhost")));

        NotParamProperty config = injector.inject(props);

        Assert.assertEquals("localhost", config.host);
        Assert.assertNull(config.hostAddress);
    }

    @Test
    public void testArrayInjection() throws Exception {
        ConfigInjector<ArrayProperty> injector = ConfigInjector.forClass(ArrayProperty.class);

        Map<String, Property> props = props(new Property("ports", new Value("8080")));
        ArrayProperty config = injector.inject(props);

        Assert.assertEquals(1, config.ports.length);
        Assert.assertEquals(8080, config.ports[0]);
    }

    @Test
    public void testListInjection() throws Exception {
        ConfigInjector<ListProperty> injector = ConfigInjector.forClass(ListProperty.class);

        Map<String, Property> props = props(new Property("ports", new Value("8080")));
        ListProperty config = injector.inject(props);

        Assert.assertEquals(1, config.ports.size());
        Assert.assertEquals(8080, config.ports.get(0).intValue());
    }

    @Test
    public void testSetInjection() throws Exception {
        ConfigInjector<SetProperty> injector = ConfigInjector.forClass(SetProperty.class);

        Map<String, Property> props = props(new Property("ports", new Value("8080")));
        SetProperty config = injector.inject(props);

        Assert.assertEquals(1, config.ports.size());
        Assert.assertEquals(8080, config.ports.iterator().next().intValue());
    }

    @Test
    public void testCollectionInjection() throws Exception {
        ConfigInjector<CollectionProperty> injector = ConfigInjector.forClass(CollectionProperty.class);

        Map<String, Property> props = props(new Property("ports", new Value("8080")));
        CollectionProperty config = injector.inject(props);

        Assert.assertEquals(1, config.ports.size());
        Assert.assertEquals(8080, config.ports.iterator().next().intValue());
    }

    @Test
    public void testUninitializedCollectionInjection() throws Exception {
        ConfigInjector<UninitializedCollectionProperty> injector = ConfigInjector.forClass(UninitializedCollectionProperty.class);

        Map<String, Property> props = props(new Property("ports", new Value("8080")));
        try {
            injector.inject(props);
            Assert.fail();
        } catch (ConfigException e) {
            Assert.assertTrue(e.getMessage().contains("Cannot infer a collection implementation"));
        }
    }

    @Test
    public void testCollectionInjectionPreservesExistingFieldValue() throws Exception {
        ConfigInjector<InitializedListProperty> injector = ConfigInjector.forClass(InitializedListProperty.class);

        Map<String, Property> props = props(new Property("ports", new Value("8080")));
        InitializedListProperty config = injector.inject(props);

        Assert.assertSame(InitializedListProperty.staticPorts, config.ports);
    }

    @Test
    public void testMapInjection() throws Exception {
        ConfigInjector<MapProperty> injector = ConfigInjector.forClass(MapProperty.class);

        Map<String, Property> props = props(new Property("ports", new MapEntry("first", "8080")));
        MapProperty config = injector.inject(props);

        Assert.assertEquals(1, config.ports.size());
        Assert.assertEquals(8080, config.ports.get("first").intValue());
    }

    @Test
    public void testMapInjectionPreservesExistingFieldValue() throws Exception {
        ConfigInjector<InitializedMapProperty> injector = ConfigInjector.forClass(InitializedMapProperty.class);

        Map<String, Property> props = props(new Property("ports", new MapEntry("first", "8080")));
        InitializedMapProperty config = injector.inject(props);

        Assert.assertSame(InitializedMapProperty.staticPorts, config.ports);
    }

    @Test
    public void testRawListInjectionThrowsException() throws Exception {
        ConfigInjector<RawListProperty> injector = ConfigInjector.forClass(RawListProperty.class);

        Map<String, Property> props = props(new Property("ports", new Value("8080")));

        try {
            injector.inject(props);
            Assert.fail();
        } catch (ConfigException e) {
            Assert.assertTrue(e.getCause().getMessage().contains("Cannot determine the type parameter"));
        }
    }

    @Test
    public void testCollectionOfGenericTypeInjection() throws Exception {
        ConfigInjector<StringListProperty> injector = ConfigInjector.forClass(StringListProperty.class);

        Map<String, Property> props = props(new Property("list", new Value("java.lang.String"), new Value("java.lang.StringBuffer")));
        StringListProperty config = injector.inject(props);

        Assert.assertEquals(2, config.list.size());
        Assert.assertEquals("java.lang.String", config.list.get(0));
        Assert.assertEquals("java.lang.StringBuffer", config.list.get(1));
    }

    @Test
    public void testCollectionOfClassOfGenericTypeInjection() throws Exception {
        ConfigInjector<StringClassListProperty> injector = ConfigInjector.forClass(StringClassListProperty.class);

        Map<String, Property> props = props(new Property("list", new Value("java.lang.String"), new Value("java.lang.StringBuffer")));
        StringClassListProperty config = injector.inject(props);

        Assert.assertEquals(2, config.list.size());
        Assert.assertEquals(String.class, config.list.get(0));
        Assert.assertEquals(StringBuffer.class, config.list.get(1));
    }

    @Test
    public void testAtomic() throws Exception {
        ConfigInjector<AtomicProperties> injector = ConfigInjector.forClass(AtomicProperties.class);

        Map<String, Property> props = props(new Property("port", new Value("8080")),
                new Property("counter", new Value("10000000000")),
                new Property("active", new Value("true")));
        AtomicProperties config = injector.inject(props);

        Assert.assertEquals(8080, config.port.get());
        Assert.assertEquals(10000000000L, config.counter.get());
        Assert.assertTrue(config.active.get());

    }

    @Test
    public void testInvalidAtomic() throws Exception {
        try {
            ConfigInjector.forClass(InvalidAtomicProperties.class);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // ok
            Assert.assertEquals("Property active in " +
                    "com.avast.syringe.config.internal.ConfigInjectorTest$InvalidAtomicProperties must be final",
                    e.getMessage());
        }
    }

    private Map<String, Property> props(Property... props) {
        Map<String, Property> result = Maps.newHashMap();
        for (Property prop : props) {
            result.put(prop.getName(), prop);
        }
        return result;
    }

    static class SingleProperty {

        @ConfigProperty
        int port;
    }

    static class TwoProperties {

        @ConfigProperty
        String host;
        @ConfigProperty
        int port;
    }

    static class TwoPropertiesSubclass extends SingleProperty {

        @ConfigProperty
        String host;
    }

    static class OptionalProperty {

        @ConfigProperty
        String host;
        @ConfigProperty(optional = true)
        int port = 1234;
    }

    static class NotParamProperty {

        @ConfigProperty
        String host;
        InetAddress hostAddress;
    }

    static class ArrayProperty {

        @ConfigProperty
        int[] ports;
    }

    static class ListProperty {

        @ConfigProperty
        List<Integer> ports;
    }

    static class SetProperty {

        @ConfigProperty
        Set<Integer> ports;
    }

    static class CollectionProperty {

        @ConfigProperty
        Collection<Integer> ports = new HashSet<Integer>();
    }

    static class UninitializedCollectionProperty {

        @ConfigProperty
        Collection<Integer> ports;
    }

    static class MapProperty {

        @ConfigProperty
        Map<String, Integer> ports;
    }

    static class InitializedListProperty {

        static List<Integer> staticPorts = new ArrayList<Integer>();

        @ConfigProperty
        List<Integer> ports = staticPorts;
    }

    static class InitializedMapProperty {

        static Map<String, Integer> staticPorts = new HashMap<String, Integer>();

        @ConfigProperty
        Map<String, Integer> ports = staticPorts;
    }

    static class RawListProperty {

        @ConfigProperty
        @SuppressWarnings("rawtypes")
        List ports;
    }
    
    static class StringListProperty {

        @ConfigProperty
        List<? extends CharSequence> list;
    }

    static class StringClassListProperty {

        @ConfigProperty
        List<Class<? extends CharSequence>> list;
    }

    static class AtomicProperties {

        @ConfigProperty
        private final AtomicInteger port = new AtomicInteger();

        @ConfigProperty
        private final AtomicLong counter = new AtomicLong();

        @ConfigProperty
        private final AtomicBoolean active = new AtomicBoolean();

    }

    static class InvalidAtomicProperties {

        @ConfigProperty
        AtomicBoolean active;
    }

}
