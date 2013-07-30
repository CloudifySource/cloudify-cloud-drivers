package org.cloudifysource.esc.driver.provisioning.privateEc2.parser.deserializers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.AWSEC2Instance;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.AWSEC2Volume;
import org.cloudifysource.esc.driver.provisioning.privateEc2.parser.beans.AWSResource;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.ObjectCodec;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;


public class ResourcesDeserializer extends JsonDeserializer<List<AWSResource>> {

    protected final Logger logger = Logger.getLogger(this.getClass().getName());

    public ResourcesDeserializer() {
    }

    @Override
    public List<AWSResource> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        List<AWSResource> resources = new ArrayList<AWSResource>();
        if (JsonToken.START_OBJECT != jp.getCurrentToken()) {
            throw ctxt.mappingException("Expected START OBJECT got: " + jp.getCurrentName());
        }

        // next Resources
        while (JsonToken.END_OBJECT != jp.nextToken()) {
            AWSResource deserialize = deserializeResource(jp, ctxt);
            if (deserialize != null) {
                resources.add(deserialize);
            }
        }

        return resources;
    }

    public AWSResource deserializeResource(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String resourceName = jp.getText();

        jp.nextToken();
        JsonNode jsonNode = jp.readValueAsTree();
        String type = jsonNode.path("Type").getTextValue();
        ObjectCodec codec = jp.getCodec();
        if (type != null && type.startsWith("AWS::")) {
            if ("AWS::EC2::Instance".equals(type)) {
                AWSEC2Instance instance = codec.treeToValue(jsonNode, AWSEC2Instance.class);
                instance.setResourceName(resourceName);
                return instance;
            } else if ("AWS::EC2::Volume".equals(type)) {
                AWSEC2Volume instance = codec.treeToValue(jsonNode, AWSEC2Volume.class);
                instance.setResourceName(resourceName);
                return instance;
            } else {
                logger.warning("The parsing do not handle resource type " + type);
                return null;
            }
        }
        throw ctxt.mappingException("Node is of type AWS::EC2 resources: " + resourceName);
    }
}
