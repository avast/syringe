package com.avast.syringe.config.internal;

import com.avast.syringe.config.ConfigProperty;
import com.avast.syringe.config.ConfigurationLoader;
import com.avast.syringe.config.XmlSchemaLocation;

@XmlSchemaLocation("config-test.xsd")
public class TestConfigWithContextProperty extends TestConfig {

    @ConfigProperty(habitat = ConfigProperty.Habitat.CONTEXT)
    private ConfigurationLoader configLoader;

    public ConfigurationLoader getConfigLoader() {
        return configLoader;
    }

    public void setConfigLoader(ConfigurationLoader configLoader) {
        this.configLoader = configLoader;
    }
}
