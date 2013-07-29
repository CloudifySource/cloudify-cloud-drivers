package org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types;

public class StringValue implements ValueType {

    private final String value;

    public StringValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value.toString();
    }

    @Override
    public String toString() {
        return value;
    }
}
