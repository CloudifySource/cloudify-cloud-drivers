package org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types.RefValue;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types.ValueType;
import org.codehaus.jackson.annotate.JsonProperty;


public class VolumeMapping {

    @JsonProperty("VolumeId")
    private RefValue volumeId;

    @JsonProperty("Device")
    private ValueType device;

    public ValueType getVolumeId() {
        return volumeId;
    }

    public ValueType getDevice() {
        return device;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
