package org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans;

import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.deserializers.ResourcesDeserializer;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonDeserialize;


public class PrivateEc2Template {

    @JsonProperty("Resources")
    @JsonDeserialize(using = ResourcesDeserializer.class)
    private List<AWSResource> resources;

    public void setResources(List<AWSResource> resources) {
        this.resources = resources;
    }

    public List<AWSResource> getResources() {
        return resources;
    }

    public AWSEC2Instance getEC2Instance() {
        return this.getResourceType(AWSEC2Instance.class, null);
    }

    public AWSEC2Volume getEC2Volume(String volumeName) {
        return this.getResourceType(AWSEC2Volume.class, volumeName);
    }

    private <T> T getResourceType(Class<T> clazz, String volumeName) {
        for (AWSResource resource : this.resources) {
            if (clazz.isInstance(resource)) {
                if (volumeName != null) {
                    if (volumeName.equals(resource.getResourceName())) {
                        return (T) resource;
                    }
                } else {
                    return (T) resource;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
