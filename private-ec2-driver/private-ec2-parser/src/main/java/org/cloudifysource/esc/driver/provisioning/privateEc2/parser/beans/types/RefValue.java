package org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types;


public class RefValue implements ValueType {

    private String resourceName;

    public RefValue(String resourceName) {
        this.resourceName = resourceName;
    }

    @Override
    public String getValue() {
        return this.resourceName;
    }

    @Override
    public String toString() {
        return "Ref=" + resourceName;
    }

}
