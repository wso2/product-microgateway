/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.micro.gateway.interceptor;

import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.jvm.values.api.BArray;
import org.ballerinalang.jvm.values.api.BMap;
import org.ballerinalang.jvm.values.api.BXML;
import org.ballerinalang.net.http.HttpUtil;
import org.ballerinalang.net.http.nativeimpl.ExternRequest;
import org.ballerinalang.stdlib.io.channels.base.Channel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.channels.ByteChannel;
import java.util.Map;

/**
 * Representation of ballerina http:Request object. Provide methods to do CRUD operations on the request object
 * when writing gateway interceptors.
 */
public class Request {
    private ObjectValue requestObj;
    private Entity entity;

    public Request(ObjectValue requestObj) {
        this.requestObj = requestObj;
    }

    /**
     * Gets the requested resource path
     *
     * @return Resource path value as a string
     */
    public String getRequestPath() {
        return requestObj.getStringValue(Constants.RESOURCE_PATH);
    }

    /**
     * Gets the requested resource http method
     *
     * @return Resource http method as a string. For ex: POST
     */
    public String getRequestHttpMethod() {
        return requestObj.getStringValue(Constants.REQUEST_METHOD);
    }

    /**
     * Gets the http version used for the request.
     *
     * @return Http version used for the request. For ex: 1.1 or 2.0
     */
    public String getRequestHttpVersion() {
        return requestObj.getStringValue(Constants.REQUEST_HTTP_VERSION);
    }

    /**
     * Gets the additional path information available in the request
     *
     * @return Extra path information as a string
     */
    public String getPathInfo() {
        return requestObj.getStringValue(Constants.PATH_INFO);
    }

    /**
     * Gets the request user agent details.
     *
     * @return User agent header details as the string.
     */
    public String getUserAgent() {
        return requestObj.getStringValue(Constants.USER_INFO);
    }

    /**
     * Gets the query parameters of the request as a ballerina map.
     *
     * @return {@link BMap} Ballerina map value object containing query parameters.
     */
    public Map<String, String> getQueryParams() {
        return InterceptorUtils.convertBMapToMap(getNativeQueryParams());
    }

    private BMap<String, Object> getNativeQueryParams() {
        return ExternRequest.getQueryParams(requestObj);
    }

    /**
     * Gets the query param value associated with the given key.
     *
     * @param key Represents the query param key.
     * @return Returns the query param value associated with the given key as a string. If multiple param values are
     * present, then the first value is returned. Null is returned if no key is found.
     */
    public String getQueryParamValue(String key) {
        BMap mapValue = getNativeQueryParams();
        BArray arrayValue = ((MapValue) mapValue).getArrayValue(key);
        if (arrayValue != null) {
            return arrayValue.get(0).toString();
        }
        return null;
    }

    /**
     * Gets all the query param values associated with the given key.
     *
     * @param key Represents the query param key.
     * @return Returns all the query param values associated with the given key as a `String[]`.
     * Null is returned if no key is found.
     */
    public String[] getQueryParamValues(String key) {
        BMap mapValue = getNativeQueryParams();
        BArray arrayValue = ((MapValue) mapValue).getArrayValue(key);
        if (arrayValue != null) {
            return arrayValue.getStringArray();
        }
        return null;
    }

    /**
     * Gets the matrix parameters of the request.
     *
     * @param path Path to the location of matrix parameters.
     * @return A map value object {@link MapValue} of matrix parameters which can be found for the given path.
     */
    public Map<String, String> getMatrixParams(String path) {

        return InterceptorUtils.convertBMapToMap(ExternRequest.getMatrixParams(requestObj, path));
    }

    /**
     * Checks whether the requested header key exists in the header map.
     *
     * @param headerName The header name.
     * @return Returns true if the specified header key exists
     */
    public boolean hasHeader(String headerName) {
        return getEntityWithoutBody().hasHeader(headerName);
    }

    /**
     * Returns the value of the specified header. If the specified header key maps to multiple values, the first of
     * these values is returned.
     *
     * @param headerName The header name.
     * @return The first header value for the specified header name. Null is returned if header does not present.
     */
    public String getHeader(String headerName) {
        return getEntityWithoutBody().getHeader(headerName);
    }

    /**
     * Gets all the header values to which the specified header key maps to.
     *
     * @param headerName The header name.
     * @return The header values the specified header key maps to. Null is returned if header does not present.
     */
    public String[] getHeaders(String headerName) {
        return getEntityWithoutBody().getHeaders(headerName);
    }

    /**
     * Sets the specified header to the request. If a mapping already exists for the specified header key, the existing
     * header value is replaced with the specified header value.
     *
     * @param headerName  The header name.
     * @param headerValue The header value.
     */
    public void setHeader(String headerName, String headerValue) {
        getEntityWithoutBody().setHeader(headerName, headerValue);
    }

    /**
     * Adds the specified header to the request. Existing header values are not replaced.
     *
     * @param headerName  The header name.
     * @param headerValue The header value.
     */
    public void addHeader(String headerName, String headerValue) {
        getEntityWithoutBody().addHeader(headerName, headerValue);
    }

    /**
     * Removes the specified header from the request.
     *
     * @param headerName The header name.
     */
    public void removeHeader(String headerName) {
        getEntityWithoutBody().removeHeader(headerName);
    }

    /**
     * Removes all the headers from the request.
     */
    public void removeAllHeaders() {
        getEntityWithoutBody().removeAllHeaders();
    }

    /**
     * Gets all the names of the headers of the request.
     *
     * @return An array of all the header names.
     */
    public String[] getHeaderNames() {
        return getEntityWithoutBody().getHeaderNames();
    }

    /**
     * Checks whether the client expects a `100-continue` response.
     *
     * @return Returns true if the client expects a `100-continue` response.
     */
    public boolean expects100Continue() {
        return hasHeader(Constants.EXPECT_HEADER) && Constants.HUNDRED_CONTINUE
                .equals(getHeader(Constants.EXPECT_HEADER));
    }

    /**
     * Sets the `content-type` header to the request.
     *
     * @param contentType Content type value to be set as the `content-type` header.
     */
    public void setContentType(String contentType) {
        setHeader(Constants.CONTENT_TYPE_HEADER, contentType);

    }

    /**
     * Gets the type of the payload of the request (i.e: the `content-type` header value).
     *
     * @return Returns the `content-type` header value as a string. If not present returns empty string
     */
    public String getContentType() {
        return hasHeader(Constants.CONTENT_TYPE_HEADER) ? getHeader(Constants.CONTENT_TYPE_HEADER) : "";
    }

    /**
     * Extracts `json` payload from the request. If the content type is not JSON, an exception will be thrown.
     *
     * @return The `json` {@link JSONObject} payload of the request. Null if json body is not found in the request.
     * @throws InterceptorException If error while getting json payload.
     */
    public JSONObject getJsonPayload() throws InterceptorException {
        return getEntity().getJson();
    }

    /**
     * Extracts `json array` payload from the request. If the content type is not JSON, an exception will be thrown.
     *
     * @return The `json` {@link JSONArray} payload of the request. Null if json body is not found in the request.
     * @throws InterceptorException If error while getting json payload.
     */
    public JSONArray getJsonArrayPayload() throws InterceptorException {
        return getEntity().getJsonArray();
    }

    /**
     * Extracts `xml` payload from the request. If the content type is not XML, an exception will be thrown.
     *
     * @return {@link BXML} The `xml` payload of the request.
     * @throws InterceptorException If error while getting xml payload.
     */
    public BXML getXmlPayload() throws InterceptorException {
        return getEntity().getXml();
    }

    /**
     * Extracts `text` payload from the request. If the content type is not text, an exception will be thrown.
     *
     * @return The `text` payload of the request.
     * @throws InterceptorException If error while getting text payload.
     */
    public String getTextPayload() throws InterceptorException {
        return getEntity().getText();
    }

    /**
     * Gets the request payload as a `ByteChannel` except in the case of multiparts. To retrieve multiparts, use
     * `Request.getBodyParts()`.
     *
     * @return {@link ByteChannel} A byte channel from which the message payload can be read.
     * @throws InterceptorException If error while getting byte channel of the request.
     */
    public ByteChannel getByteChannel() throws InterceptorException {
        return getEntity().getByteChannel();
    }

    /**
     * Gets the request payload as a `byte[]`.
     *
     * @return The byte[] representation of the message payload
     * @throws InterceptorException If error while getting byte array of the request.
     */
    public byte[] getBinaryPayload() throws InterceptorException {
        return getEntity().getByteArray();
    }

    /**
     * Extracts body parts from the request. If the content type is not a composite media type, an exception
     * is thrown.
     *
     * @return Returns the body parts as an array of entities.
     * @throws InterceptorException if there were any errors  constructing the body parts from the request.
     */
    public Entity[] getBodyParts() throws InterceptorException {
        return getEntity().getBodyParts();
    }

    /**
     * Sets a json {@link JSONObject} as the payload to the request.
     *
     * @param jsonPayload {@link JSONObject} The json payload.
     */
    public void setJsonPayload(JSONObject jsonPayload) {
        getEntityWithoutBody().setJson(jsonPayload);
        setEntity(entity);
    }

    /**
     * Sets a json array {@link JSONArray} as the payload to the request.
     *
     * @param jsonArrayPayload {@link JSONArray} The json array payload.
     */
    public void setJsonPayload(JSONArray jsonArrayPayload) {
        getEntityWithoutBody().setJson(jsonArrayPayload);
        setEntity(entity);
    }

    /**
     * Sets a xml as the payload.
     *
     * @param xmlPayload The xml {@link BXML} payload.
     */
    public void setXmlPayload(BXML xmlPayload) {
        getEntityWithoutBody().setXml(xmlPayload);
        setEntity(entity);
    }

    /**
     * Sets a string text content as the payload.
     *
     * @param textPayload The text payload.
     */
    public void setTextPayload(String textPayload) {
        getEntityWithoutBody().setText(textPayload);
        setEntity(entity);
    }

    /**
     * Sets a byte[] content as the payload.
     *
     * @param binaryPayload The byte[] payload.
     */
    public void setBinaryPayload(byte[] binaryPayload) {
        getEntityWithoutBody().setBinary(binaryPayload);
        setEntity(entity);
    }

    /**
     * Set multiparts as the payload.
     *
     * @param bodyParts   The entities which make up the message body
     * @param contentType The content type of the top level message. Set this to override the default
     *                    `content-type` header value which is 'multipart/form-data'
     */
    public void setBodyParts(Entity[] bodyParts, String contentType) {
        getEntityWithoutBody().setBodyParts(bodyParts, contentType);
        setEntity(entity);
    }

    /**
     * Set byte channel as the payload.
     *
     * @param byteChannel {@link Channel} Channel object which contains the payload data.
     * @param contentType The content type of the top level message. Set this to override the default
     *                    `content-type` header value which is 'application/octet-stream'
     */
    public void setByteChannel(Channel byteChannel, String contentType) {
        getEntityWithoutBody().setByteChannel(byteChannel, contentType);
        setEntity(entity);
    }

    /**
     * Returns the java native object of the ballerina level http:Request object.
     *
     * @return Native ballerina object {@link ObjectValue} representing the request.
     */
    public ObjectValue getNativeRequestObject() {
        return requestObj;
    }

    public void setEntity(Entity entity) {
        ExternRequest.setEntity(requestObj, entity.getEntityObj());
    }

    public Entity getEntityWithoutBody() {
        entity = new Entity(HttpUtil.getEntity(requestObj, true, false));
        return entity;
    }

    public Entity getEntity() {
        entity = new Entity(HttpUtil.getEntity(requestObj, true, true));
        return entity;
    }

}
