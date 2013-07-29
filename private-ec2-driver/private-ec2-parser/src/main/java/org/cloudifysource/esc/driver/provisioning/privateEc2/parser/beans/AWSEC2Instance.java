package org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans;

import org.codehaus.jackson.annotate.JsonProperty;

public class AWSEC2Instance extends AWSResource {

    @JsonProperty("Properties")
    private InstanceProperties properties;

    public InstanceProperties getProperties() {
        return properties;
    }
}
