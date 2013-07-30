package org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types;

import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class JoinFunction implements ValueType {

    private final String separator;

    private final List<ValueType> strings;

    public JoinFunction(String separator, List<ValueType> strings) {
        this.separator = separator;
        this.strings = strings;
    }

    @Override
    public String getValue() {
        StringBuilder sb = new StringBuilder();
        for (ValueType s : this.strings) {
            sb.append(s.getValue()).append(this.separator);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
