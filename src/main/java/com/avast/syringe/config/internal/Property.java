package com.avast.syringe.config.internal;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class Property {

    private final String name;
    private final List<Value> values;

    public Property(String name, List<Value> values) {
        this.name = name;
        this.values = ImmutableList.copyOf(values);
    }

    public Property(String name, Value... values) {
        this.name = name;
        this.values = ImmutableList.copyOf(values);
    }

    public Property(String name, Value value) {
        this.name = name;
        this.values = ImmutableList.of(value);
    }

    public Property(String name) {
        this.name = name;
        this.values = ImmutableList.of();
    }

    public String getName() {
        return name;
    }

    public List<Value> getValues() {
        return values;
    }

    @Override
    public String toString() {
        return String.format("Property[name=%s, values=%s]", name, values);
    }
}
