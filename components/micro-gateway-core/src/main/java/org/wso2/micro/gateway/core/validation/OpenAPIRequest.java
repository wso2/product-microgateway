/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.micro.gateway.core.validation;

import com.atlassian.oai.validator.model.Request;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.validation.constraints.NotNull;

/**
 * Request Model class for OpenAPI
 */
public class OpenAPIRequest implements Request {

    private final Method method;
    private final Multimap<String, String> headers = ArrayListMultimap.create();
    private String path;
    private Map<String, Collection<String>> queryParams;
    private Optional<String> requestBody;

    /**
     * Build OAI Request from Message Context.
     *
     * @param requestPath      API request resource path
     * @param reqMethod        API request method
     * @param requestBody      Request body
     * @param transportHeaders Transport headers
     * @param swagger          Swagger definition
     */
    public OpenAPIRequest(String requestPath, String reqMethod, String requestBody,
                          Multimap<String, String> transportHeaders, Map<String, Collection<String>> queryParams,
                          String swagger) {
        this.method = Method.valueOf(reqMethod.toUpperCase());
        this.path = requestPath;
        this.requestBody = Optional.of(requestBody);
        if (swagger != null) {
            OpenAPIParser openAPIParser = new OpenAPIParser();
            SwaggerParseResult swaggerParseResult = openAPIParser.readContents(swagger, new ArrayList<>(),
                    new ParseOptions());
            OpenAPI openAPI = swaggerParseResult.getOpenAPI();
            validatePath(openAPI);
        }
        this.headers.putAll(transportHeaders);
        this.queryParams = queryParams;
    }

    @NotNull
    @Override
    public String getPath() {
        return this.path;
    }

    @NotNull
    @Override
    public Method getMethod() {
        return this.method;
    }

    @NotNull
    @Override
    public Optional<String> getBody() {
        return this.requestBody;
    }

    @NotNull
    @Override
    public Collection<String> getQueryParameters() {
        if (this.queryParams == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(this.queryParams.keySet());
    }

    @NotNull
    @Override
    public Collection<String> getQueryParameterValues(String s) {
        if (this.queryParams == null) {
            return Collections.emptyList();
        }
        return SchemaValidationUtils.getFromMapOrEmptyList(this.queryParams, s);
    }

    @NotNull
    @Override
    public Map<String, Collection<String>> getHeaders() {
        if (this.headers == null) {
            return Collections.emptyMap();
        }
        return headers.asMap();
    }

    @NotNull
    @Override
    public Collection<String> getHeaderValues(String s) {
        if (this.headers == null) {
            return Collections.emptyList();
        }
        return SchemaValidationUtils.getFromMapOrEmptyList(this.headers.asMap(), s);
    }

    protected void validatePath(OpenAPI openAPI) {
        Paths paths = openAPI.getPaths();
        if (path.equals("/") && !paths.containsKey(path)) {
            if (paths.containsKey("/*")) {
                path = "/*";
            }
        }
    }
}
