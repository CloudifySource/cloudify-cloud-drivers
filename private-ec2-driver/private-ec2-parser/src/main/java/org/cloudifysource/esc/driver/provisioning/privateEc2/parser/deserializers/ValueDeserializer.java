package org.cloudifysource.esc.driver.provisioning.privateEc2.parser.deserializers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types.Base64Function;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types.JoinFunction;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types.RefValue;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types.StringValue;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.types.ValueType;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.ObjectCodec;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonMappingException;


public class ValueDeserializer extends JsonDeserializer<ValueType> {
    private static final Logger logger = Logger.getLogger(ValueDeserializer.class.getName());

    @Override
    public ValueType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ObjectCodec oc = jp.getCodec();
        JsonNode node = oc.readTree(jp);
        return this.functionValue(node, ctxt);
    }

    private ValueType functionValue(JsonNode root, DeserializationContext ctxt) throws IOException, JsonProcessingException, JsonMappingException {

        Iterator<String> fieldNames = root.getFieldNames();

        while (fieldNames.hasNext()) {
            String next = fieldNames.next();

            if ("Fn::Base64".equals(next)) {
                JsonNode jsonNode = root.get(next);
                ValueType value = this.functionValue(jsonNode, ctxt);
                return new Base64Function(value);
            } else if ("Fn::Join".equals(next)) {
                JsonNode joinNode = root.get(next);
                Iterator<JsonNode> elements = joinNode.getElements();

                JsonNode separatorNode = elements.next();
                String separator = separatorNode.getTextValue();

                JsonNode toJoinNodes = elements.next();
                Iterator<JsonNode> iterator = toJoinNodes.iterator();
                List<ValueType> toJoinList = new ArrayList<ValueType>();
                while (iterator.hasNext()) {
                    JsonNode node = iterator.next();
                    toJoinList.add(this.functionValue(node, ctxt));
                }

                return new JoinFunction(separator, toJoinList);
            } else if ("Ref".equals(next)) {
                return new RefValue(root.get(next).getValueAsText());
            } else {
                logger.warning("Value not supported: " + next + " - node: " + root.toString());
                return new StringValue(root.get(next).getValueAsText());
            }
        }
        return new StringValue(root.getTextValue());
    }
}
