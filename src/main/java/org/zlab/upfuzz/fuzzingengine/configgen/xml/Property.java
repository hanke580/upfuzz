package org.zlab.upfuzz.fuzzingengine.configgen.xml;

public class Property {
    private String name;
    private String value;

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Property [name=" + name + ", value=" + value + "]";
    }
}
