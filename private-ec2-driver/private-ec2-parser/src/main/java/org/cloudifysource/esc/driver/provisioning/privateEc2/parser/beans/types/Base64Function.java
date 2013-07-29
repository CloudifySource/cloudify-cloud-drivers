package org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class Base64Function implements ValueType {

    private final ValueType toEncode;

    public Base64Function(ValueType toEncode) {
        this.toEncode = toEncode;
    }

    @Override
    public String getValue() {
        return this.toEncode.getValue();
    }

    public String getEncodedValue() {
        return StringUtils.newStringUtf8(Base64.encodeBase64(toEncode.getValue().getBytes()));
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
