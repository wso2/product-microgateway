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

import com.atlassian.oai.validator.model.Request.Method;
import com.atlassian.oai.validator.model.Response;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Optional;

import javax.validation.constraints.NotNull;

/**
 * Response Model class for OpenAPI
 */
public class OpenAPIResponse implements Response {

    private final int status;
    private final Optional<String> responseBody;
    private final Multimap<String, String> headers = ArrayListMultimap.create();
    private final Method method;
    private final String path;

    /**
     * Build OAI Response from messageContext.
     *
     * @param resourcePath     Resource path
     * @param reqMethod        Request method
     * @param responseCode     Response code
     * @param responseBody     Response body
     * @param transportHeaders Transport headers
     */
    public OpenAPIResponse(String resourcePath, String reqMethod, String responseCode, String responseBody,
                           Multimap<String, String> transportHeaders) {
        int statusCode = Integer.parseInt(responseCode);
        this.status = statusCode;
        this.method = Method.valueOf(reqMethod.toUpperCase());
        this.path = resourcePath;
        this.responseBody = Optional.of(responseBody);
        this.headers.putAll(transportHeaders);
    }

    @Override
    public int getStatus() {
        return status;
    }

    /**
     * @deprecated
     */
    @NotNull
    @Override
    public Optional<String> getBody() {
        return responseBody;
    }

    @NotNull
    @Override
    public Collection<String> getHeaderValues(String s) {
        return SchemaValidationUtils.getFromMapOrEmptyList(headers.asMap(), s);
    }

    public Method getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }
}
