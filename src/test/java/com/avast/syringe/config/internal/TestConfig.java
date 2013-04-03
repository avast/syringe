package com.avast.syringe.config.internal;

import java.util.List;
import java.util.Map;

import com.avast.syringe.config.ConfigProperty;
import com.avast.syringe.config.XmlSchemaLocation;

@XmlSchemaLocation("config-test.xsd")
public class TestConfig {

    @ConfigProperty
    private int port;
    @ConfigProperty
    private List<Integer> list;
    @ConfigProperty
    private Map<String, Long> map;

    public int getPort() {
        return port;
    }

    public List<Integer> getList() {
        return list;
    }

    public Map<String, Long> getMap() {
        return map;
    }
}
