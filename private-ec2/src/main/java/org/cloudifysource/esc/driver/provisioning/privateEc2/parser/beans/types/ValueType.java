package org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types;

import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.deserializers.ValueDeserializer;
import org.codehaus.jackson.map.annotate.JsonDeserialize;


@JsonDeserialize(using = ValueDeserializer.class)
public interface ValueType {

    public String getValue();
}
