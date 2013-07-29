package org.cloudifysource.esc.driver.provisioning.privateEc2.parser;

import java.io.File;
import java.io.InputStream;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper;

public class ParserUtils {

    public static <T> T mapJson(Class<T> clazz, InputStream jsonStream) throws PrivateEc2ParserException {
        if (jsonStream == null) {
            return null;
        }

        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(Feature.USE_ANNOTATIONS, true);
        mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        T tokenResponse = null;
        try {
            tokenResponse = mapper.readValue(jsonStream, clazz);
        } catch (Exception e) {
            throw new PrivateEc2ParserException(e);
        }
        return tokenResponse;
    }

    public static <T> T mapJson(Class<T> clazz, String jsonString) throws PrivateEc2ParserException {
        if (jsonString == null) {
            return null;
        }

        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(Feature.USE_ANNOTATIONS, true);
        mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        T tokenResponse = null;
        try {
            tokenResponse = mapper.readValue(jsonString, clazz);
        } catch (Exception e) {
            throw new PrivateEc2ParserException(e);
        }
        return tokenResponse;
    }

    public static <T> T mapJson(Class<T> clazz, File file) throws PrivateEc2ParserException {
        if (file == null) {
            return null;
        }
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(Feature.USE_ANNOTATIONS, true);
        mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        T tokenResponse = null;
        try {
            tokenResponse = mapper.readValue(file, clazz);
        } catch (Exception e) {
            throw new PrivateEc2ParserException(e);
        }
        return tokenResponse;
    }

    public static <T> T mapJson(Class<T> clazz, JsonNode jsonNode) throws PrivateEc2ParserException {
        if (jsonNode == null) {
            return null;
        }
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(Feature.USE_ANNOTATIONS, true);
        mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        T tokenResponse = null;
        try {
            tokenResponse = mapper.readValue(jsonNode, clazz);
        } catch (Exception e) {
            throw new PrivateEc2ParserException(e);
        }
        return tokenResponse;
    }

}
