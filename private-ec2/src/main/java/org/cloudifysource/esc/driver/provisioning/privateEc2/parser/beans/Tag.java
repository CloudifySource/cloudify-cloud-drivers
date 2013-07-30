package org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.codehaus.jackson.annotate.JsonProperty;

public class Tag {

    @JsonProperty("Key")
    private String key;

    @JsonProperty("Value")
    private String value;

    public Tag() {
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public com.amazonaws.services.ec2.model.Tag convertToEC2Model() {
        return new com.amazonaws.services.ec2.model.Tag(key, value);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
