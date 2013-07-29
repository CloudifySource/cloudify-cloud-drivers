package org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types.ValueType;
import org.codehaus.jackson.annotate.JsonProperty;


public class InstanceProperties {

    @JsonProperty("AvailabilityZone")
    private ValueType availabilityZone;

    @JsonProperty("ImageId")
    private ValueType imageId;

    @JsonProperty("InstanceType")
    private ValueType instanceType;

    @JsonProperty("KeyName")
    private ValueType keyName;

    @JsonProperty("PrivateIpAddress")
    private ValueType privateIpAddress;

    @JsonProperty("SecurityGroupsIds")
    private List<ValueType> securityGroupIds;

    @JsonProperty("SecurityGroups")
    private List<ValueType> securityGroups;

    @JsonProperty("Tags")
    private List<Tag> Tags;

    @JsonProperty("UserData")
    private ValueType userData;

    @JsonProperty("Volumes")
    private List<VolumeMapping> volumes;

    public ValueType getImageId() {
        return imageId;
    }

    public ValueType getInstanceType() {
        return instanceType;
    }

    public ValueType getAvailabilityZone() {
        return availabilityZone;
    }

    public List<ValueType> getSecurityGroupIds() {
        return securityGroupIds;
    }

    public List<String> getSecurityGroupIdsAsString() {
        if (securityGroupIds == null) {
            return null;
        } else {
            ArrayList<String> arrayList = new ArrayList<String>(this.securityGroupIds.size());
            for (ValueType value : securityGroupIds) {
                arrayList.add(value.getValue());
            }
            return arrayList;
        }
    }

    public List<ValueType> getSecurityGroups() {
        return securityGroups;
    }

    public List<String> getSecurityGroupsAsString() {
        if (securityGroups == null) {
            return null;
        } else {
            ArrayList<String> arrayList = new ArrayList<String>(this.securityGroups.size());
            for (ValueType value : securityGroups) {
                arrayList.add(value.getValue());
            }
            return arrayList;
        }
    }

    public ValueType getKeyName() {
        return keyName;
    }

    public List<VolumeMapping> getVolumes() {
        return volumes;
    }

    public ValueType getUserData() {
        return userData;
    }

    public ValueType getPrivateIpAddress() {
        return privateIpAddress;
    }

    public List<Tag> getTags() {
        return Tags;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
