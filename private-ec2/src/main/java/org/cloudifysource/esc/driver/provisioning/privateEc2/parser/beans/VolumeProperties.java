package org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans;

import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types.StringValue;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types.ValueType;
import org.codehaus.jackson.annotate.JsonProperty;


public class VolumeProperties {

    @JsonProperty("AvailabilityZone")
    private ValueType availabilityZone;

    @JsonProperty("Iops")
    private Integer iops;

    @JsonProperty("Size")
    private Integer size;

    @JsonProperty("SnapshotId")
    private ValueType snapshotId;

    @JsonProperty("Tags")
    private List<Tag> Tags;

    @JsonProperty("VolumeType")
    private ValueType VolumeType;

    public Integer getSize() {
        return size;
    }

    public ValueType getAvailabilityZone() {
        return availabilityZone;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = new StringValue(availabilityZone);
    }

    public Integer getIops() {
        return iops;
    }

    public ValueType getSnapshotId() {
        return snapshotId;
    }

    public List<Tag> getTags() {
        return Tags;
    }

    public ValueType getVolumeType() {
        return VolumeType;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
