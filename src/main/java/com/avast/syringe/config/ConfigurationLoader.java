package com.avast.syringe.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avast.syringe.config.fm.PropertiesLoader;
import com.avast.syringe.config.internal.ConfigInjector;
import com.avast.syringe.config.internal.InjectableProperty;
import com.avast.syringe.config.internal.Injection;
import com.avast.syringe.config.internal.Property;
import com.avast.syringe.config.internal.Value;
import com.avast.syringe.config.internal.XmlConfig;
import com.avast.syringe.config.internal.XmlConfigParser;
import com.avast.syringe.config.mbean.ConfigDynamicBean;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * {@link #load(File, Class) Loads } the values of a configuration file into
 * an instance of the passed configuration bean class.
 */
public final class ConfigurationLoader implements InstanceManager {

    private static Logger LOGGER = LoggerFactory.getLogger(ConfigurationLoader.class);

    /**
     * Predefined property name for config file name.
     */
    public static final String CONFIG_FILE_NAME_PROP = "_configFileName_";

    private final File configDir;
    private final Map<String, Object> instanceCache = Maps.newHashMap();

    private final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

    /**
     * The FreeMarker configuration object
     */
    private final File fmPropertiesFile;
    private final Configuration fmCfg;
    private final Map fmGlobalProperties;
    private final String appPropsFileName = "application.properties";
    private boolean registerMBeans = true;

    public ConfigurationLoader(File configDir) throws IOException {
        Preconditions.checkNotNull(configDir, "configDir");
        this.configDir = configDir;
        this.fmPropertiesFile = null;

        // FreeMarker initialization
        fmCfg = new Configuration();
        fmCfg.setDirectoryForTemplateLoading(configDir);
        fmCfg.setObjectWrapper(new DefaultObjectWrapper());

        fmGlobalProperties = loadFreeMarkerGlobalProperties();
    }
    
    public ConfigurationLoader(File configDir, File fmPropertiesFile) throws IOException {
        Preconditions.checkNotNull(configDir, "configDir");
        this.configDir = configDir;
        this.fmPropertiesFile = fmPropertiesFile;

        // FreeMarker initialization
        fmCfg = new Configuration();
        fmCfg.setDirectoryForTemplateLoading(configDir);
        fmCfg.setObjectWrapper(new DefaultObjectWrapper());

        fmGlobalProperties = loadFreeMarkerGlobalProperties();
    }

    private Map loadFreeMarkerGlobalProperties() throws IOException {
        String propFileName = appPropsFileName;
        File appPropsFile = null;
        if (fmPropertiesFile != null) {
            appPropsFile = fmPropertiesFile;
        } else {
            appPropsFile = new File(configDir, propFileName);
        }

        if (!appPropsFile.exists()) {
            LOGGER.warn("{} not found", appPropsFile.getAbsolutePath());
            return null;
        }

        Properties properties = new Properties();
        properties.load(new FileReader(appPropsFile));

        return PropertiesLoader.load(properties);
    }

    public <T> T load(Class<T> configClass) throws Exception {
        Preconditions.checkNotNull(configClass, "clazz");
        return load(configClass, configClass.getSimpleName() + ".xml");
    }

    public <T> T load(final String configFileName) throws Exception {
        return load(configFileName, null, null, null);
    }

    public <T> T load(String configFileName, final Injection.ContextualPropertyResolver customResolver,
                      final PropertyValueConverter converter, final @Nullable Function<Object, Object> enhancer) throws Exception {
        return load(configFileName, null, customResolver, converter, enhancer);
    }

    public <T> T load(String configFileName, File contextDir, final Injection.ContextualPropertyResolver customResolver,
                      final PropertyValueConverter converter, final @Nullable Function<Object, Object> enhancer) throws Exception {
        Preconditions.checkNotNull(configFileName, "file");

        File configFilePath = new File(contextDir, configFileName);
        String instanceKey = stripExtension(configFilePath.getCanonicalPath());

        @SuppressWarnings("unchecked")
        T instance = (T) instanceCache.get(instanceKey);
        if (instance != null) {
            return instance;
        }

        instance = createInstance(configFileName, contextDir, customResolver, converter, enhancer, registerMBeans, null);

        instanceCache.put(instanceKey, instance);

        return instance;


    }

    private <T> T createInstance(String configFileName, File contextDir,
                                 final Injection.ContextualPropertyResolver customResolver,
                                 final PropertyValueConverter converter,
                                 final Function<Object, Object> enhancer, boolean doRegisterMBeans,
                                 Function<ConfigInjector, Void> configInjectorCallback) throws Exception {

        contextDir = contextDir == null ? configDir : contextDir;
        configFileName = completeFileName(configFileName, contextDir);

        LOGGER.info("Loading XML configuration from {}", configFileName);

        T instance;
        XmlConfigParser xmlConfigParser = new XmlConfigParser();
        InputStream input;
        final File configFile = new File(contextDir, configFileName);
        if (configFileName.endsWith(".ftl")) {
            // the input file is a FreeMarker template, so process it
            // TODO:could be used normalize from file
            // configFileName can be in form ../configuration/Template.ftl, we have to remove ../ so fm can recognize path
            String trimedConfigFileName = configFileName;
            while(trimedConfigFileName.startsWith("../")) {
                trimedConfigFileName = trimedConfigFileName.substring(3);
            }
            input = processFreeMarkerTemplate(trimedConfigFileName);
        } else {
            input = new FileInputStream(configFile);
        }

        final XmlConfig xmlConfig;
        try {
            xmlConfig = xmlConfigParser.loadConfig(input);
        } finally {
            input.close();
        }

        Class<?> cls = Thread.currentThread().getContextClassLoader().loadClass(xmlConfig.getClassName());
        ConfigInjector<?> configInjector = ConfigInjector.forClass(cls, converter);

        final String cfgFileName = configFileName;
        instance = (T) configInjector.inject(xmlConfig.getProperties(), new Injection.ContextualPropertyResolver() {
            @Override
            public Object getProperty(InjectableProperty property, Value configValue) throws Exception {
                // Allow injection of this configuration loader
                if (ConfigurationLoader.class.isAssignableFrom(property.getType())) {
                    return ConfigurationLoader.this;
                }

                if (CONFIG_FILE_NAME_PROP.equals(property.getName())) {
                    return cfgFileName;
                }

                String refConfigFile = configValue != null ? configValue.getValue() : null;
                if (refConfigFile != null) {
                    return getReferencedObject(refConfigFile, configFile.getParentFile(), customResolver, converter,
                            enhancer);
                }

                if (customResolver != null) {
                    try {
                        return customResolver.getProperty(property, configValue);
                    } catch (NoSuchFieldException e) {
                        // ok
                    }
                }

                throw new NoSuchFieldException(property.getName());

            }
        });

        T decoratedInstance = instance;
        instance = decorateInstance(decoratedInstance, xmlConfig.getDecorators(), configFile.getParentFile(), customResolver,
                converter, enhancer);

        configInjector.notifyPostConstruct(decoratedInstance);

        if (doRegisterMBeans) {
            registerMBean(configFileName, instance, xmlConfig, configInjector);
        }

        if (enhancer != null) {
            instance = (T) enhancer.apply(instance);
        }

        if (configInjectorCallback != null) {
            configInjectorCallback.apply(configInjector);
        }

        return instance;
    }

    private <T> T decorateInstance(T instance, List<XmlConfig.Decorator> decorators, File contextDir,
                                   final Injection.ContextualPropertyResolver customResolver,
                                   final PropertyValueConverter converter,
                                   final Function<Object, Object> enhancer) throws Exception {
        if (decorators.isEmpty()) {
            return instance;
        }

        XmlConfig.Decorator decoratorDesc = decorators.get(0);
        final AtomicReference<Injection> delegateInjectionRef = new AtomicReference<Injection>();

        T decorator = createInstance(decoratorDesc.getName(), contextDir, customResolver, converter, enhancer, false,
                // this callback function looks for the delegate config property in the config injector
                new Function<ConfigInjector, Void>() {
                    @Override
                    public Void apply(@Nullable ConfigInjector configInjector) {

                        List<Injection> injections = configInjector.getInjections();
                        for (Injection injection : injections) {

                            if (injection.getProperty().isDelegate()) {
                                if (delegateInjectionRef.get() != null) {
                                    String message = String.format("Decorator class %s must contain exactly one " +
                                            "'delegate=true' config property",
                                            configInjector.getConfigClass());
                                    throw new IllegalArgumentException(message);
                                } else {
                                    delegateInjectionRef.set(injection);
                                }
                            }
                        }

                        return null;
                    }
                });


        // Connect the decorator with the decorated instance
        delegateInjectionRef.get().getProperty().setValue(decorator, instance);

        return decorateInstance(decorator, decorators.subList(1, decorators.size()), contextDir, customResolver,
                converter, enhancer);
    }

    private <T> void registerMBean(String configFileName, T instance, XmlConfig xmlConfig, ConfigInjector<?> configInjector) throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        // JMX - register the configuration MBean
        ConfigDynamicBean configDynamicBean =
                new ConfigDynamicBean(instance, xmlConfig.getClassName(), "", configInjector.getInjections(),
                        this);
        ObjectName configMBeanName = new ObjectName("com.avast.syringe.instances:type=" + xmlConfig.getClassName()
                + ",name=" + configFileName);
        mBeanServer.registerMBean(configDynamicBean, configMBeanName);
    }

    private String completeFileName(String configFileName, File contextDir) throws FileNotFoundException {
        try {
            return completeFileName_(configFileName, contextDir);
        } catch (FileNotFoundException e) {
            return completeFileName_(configFileName, configDir);
        }
    }

    private String completeFileName_(String configFileName, File contextDir) throws FileNotFoundException {
        File configFile = new File(contextDir, configFileName);
        if (configFile.exists()) {
            return configFileName;
        }

        configFile = new File(contextDir, configFileName + ".xml");
        if (configFile.exists()) {
            return configFileName + ".xml";
        }

        configFile = new File(contextDir, configFileName + ".ftl");
        if (configFile.exists()) {
            return configFileName + ".ftl";
        }

        throw new FileNotFoundException("No config file " + configFileName);
    }

    private InputStream processFreeMarkerTemplate(String configFileTemplate) throws IOException, TemplateException {
        Preconditions.checkNotNull(fmGlobalProperties, "No % file found in the config directory", appPropsFileName);

        Template template = fmCfg.getTemplate(configFileTemplate);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        OutputStreamWriter out = new OutputStreamWriter(bos);
        template.process(fmGlobalProperties, out);
        out.close();

        return new ByteArrayInputStream(bos.toByteArray());
    }

    private Object getReferencedObject(String refConfigFile, File contextDir,
                                       Injection.ContextualPropertyResolver customResolver,
                                       PropertyValueConverter converter,
                                       @Nullable Function<Object, Object> enhancer) throws Exception {
//        refConfigFile = stripExtension(refConfigFile);
//
//        Object instance = instanceCache.get(refConfigFile);
//        if (instance != null) {
//            return instance;
//        }

        return load(refConfigFile, contextDir, customResolver, converter, enhancer);
    }

    public String findRefName(Object ref) {
        if (ref == null) {
            return null;
        }

        for (Map.Entry<String, Object> instCacheEntry : instanceCache.entrySet()) {
            if (ref.equals(instCacheEntry.getValue())) {
                return instCacheEntry.getKey();
            }
        }
        return null;
    }

    private String stripExtension(String refConfigFile) {
        if (refConfigFile.endsWith(".xml") || refConfigFile.endsWith(".ftl")) {
            return refConfigFile.substring(0, refConfigFile.length() - 4);
        }

        return refConfigFile;
    }

    public <T> T load(Class<T> configClass, String configFileName) throws Exception {
        Preconditions.checkNotNull(configClass, "clazz");
        Preconditions.checkNotNull(configFileName, "file");

        ConfigInjector<T> configInjector = ConfigInjector.forClass(configClass);

        File configFile = new File(configDir, configFileName);
        T configBean;
        if (!configFile.exists() && !configInjector.containsMandatory()) {
            // the default config
            configBean = configClass.newInstance();
        } else {
            URL schemaUrl = getSchemaUrl(configClass);
            Map<String, Property> props = new XmlConfigParser(schemaUrl).loadProperties(configFile);
            configBean = configInjector.inject(props, new Injection.ContextualPropertyResolver() {
                @Override
                public Object getProperty(InjectableProperty property, Value configValue) throws Exception {
                    // Allow injection of this configuration loader
                    if (ConfigurationLoader.class.isAssignableFrom(property.getType())) {
                        return ConfigurationLoader.this;
                    }

//                    if (property.isReference()) {
//                        return findReference(property);
//                    }

                    throw new NoSuchFieldException(property.getName());
                }
            });
        }

        return configBean;
    }

    /**
     * Instantiate a new configuration object using a constructor and call method {@link #load(Class)}.
     */
    @Deprecated
    public static <T> T load(File file, Class<T> configClass) throws Exception {
        Preconditions.checkNotNull(file, "file");
        Preconditions.checkNotNull(configClass, "clazz");
        return new ConfigurationLoader(file.getParentFile()).load(configClass, file.getName());
    }

    private static URL getSchemaUrl(Class<?> configClass) {
        XmlSchemaLocation schemaLoc = configClass.getAnnotation(XmlSchemaLocation.class);
        if (schemaLoc == null) {
            String message = String.format("Class %s must be annotated with @XmlSchemaLocation", configClass);
            throw new IllegalArgumentException(message);
        }

        URL schemaUrl = configClass.getResource(schemaLoc.value());
        if (schemaUrl == null) {
            String message = String.format("Schema location %s for class %s not found", schemaLoc.value(), configClass);
            throw new IllegalArgumentException(message);
        }
        return schemaUrl;
    }

    public void setRegisterMBeans(boolean register) {
        this.registerMBeans = register;
    }

    public boolean isRegisterMBeans() {
        return registerMBeans;
    }
}
