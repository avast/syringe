package com.avast.syringe.config.internal;

public class MapEntry extends Value {

    private final String key;

    public MapEntry(String key, String value) {
        this(key, value, null);
    }

    public MapEntry(String key, String value, String refType) {
        super(value, refType);
        this.key = key;
    }

    public String getKey() {
        return key;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        MapEntry other = (MapEntry) obj;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("%s:%s", key, getValue());
    }
}
