package org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans;

import org.codehaus.jackson.annotate.JsonProperty;

public class AWSEC2Volume extends AWSResource {

    @JsonProperty("Properties")
    private VolumeProperties properties;

    public VolumeProperties getProperties() {
        return properties;
    }

    public void setProperties(VolumeProperties properties) {
        this.properties = properties;
    }
}
