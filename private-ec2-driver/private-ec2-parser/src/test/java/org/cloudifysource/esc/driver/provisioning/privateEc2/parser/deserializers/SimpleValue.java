package org.cloudifysource.esc.driver.provisioning.privateEc2.parser.deserializers;

import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types.ValueType;
import org.codehaus.jackson.annotate.JsonProperty;

public class SimpleValue {

    @JsonProperty("Value")
    public ValueType value;

}
