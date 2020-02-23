package org.wso2.micro.gateway.core.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.common.collect.Lists;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.wso2.micro.gateway.core.Constants;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Validate {
    private static final Log logger = LogFactory.getLog(Validate.class);
    private static JsonNode rootNode;
    private static String swaggerObject;

}
