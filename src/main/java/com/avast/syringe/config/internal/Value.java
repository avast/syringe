package com.avast.syringe.config.internal;

public class Value {

    private final String value;
    private final String refType;

    public Value(String value) {
        this(value, null);
    }

    public Value(String value, String refType) {
        this.value = value;
        this.refType = refType;
    }

    public String getValue() {
        return value;
    }

    public String getRefType() {
        return refType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Value other = (Value) obj;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }
    
    @Override
    public String toString() {
        return value;
    }
}
