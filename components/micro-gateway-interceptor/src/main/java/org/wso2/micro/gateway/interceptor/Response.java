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

import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.jvm.values.api.BXML;
import org.ballerinalang.net.http.HttpUtil;
import org.ballerinalang.net.http.ValueCreatorUtils;
import org.ballerinalang.net.http.nativeimpl.ExternResponse;
import org.ballerinalang.stdlib.io.channels.base.Channel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.channels.ByteChannel;

/**
 * Representation of ballerina http:Response object. Provide methods to do CRUD operations on the response object
 * when writing gateway interceptors.
 */
public class Response {
    private ObjectValue responseObj;
    private Entity entity;

    public Response(ObjectValue responseObj) {
        this.responseObj = responseObj;
    }

    public Response() {
        responseObj = ValueCreatorUtils.createResponseObject();
        ExternResponse.createNewEntity(responseObj);
    }

    /**
     * Get the http status code of the response. For ex: 200.
     *
     * @return Returns http status code as a long value.
     */
    public long getResponseCode() {
        return responseObj.getIntValue(Constants.STATUS_CODE);
    }

    /**
     * Set the http status code for the response. For ex: 200.
     * @param statusCode The status code to be set for the http response.
     */
    public void setResponseCode(long statusCode) {
        responseObj.set(Constants.STATUS_CODE, statusCode);
    }

    /**
     * Get the ultimate request URI that was made to receive the response when redirect is on
     * @return the requested URI
     */
    public String getResolvedRequestedURI() {
        return responseObj.getStringValue(Constants.RESOLVED_REQUESTED_URI);
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
     * Adds the specified header to the response. Existing header values are not replaced.
     *
     * @param headerName  The header name.
     * @param headerValue The header value.
     */
    public void addHeader(String headerName, String headerValue) {
        getEntityWithoutBody().addHeader(headerName, headerValue);
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
     * Sets the specified header to the response. If a mapping already exists for the specified header key, the existing
     * header value is replaced with the specified header value.
     *
     * @param headerName  The header name.
     * @param headerValue The header value.
     */
    public void setHeader(String headerName, String headerValue) {
        getEntityWithoutBody().setHeader(headerName, headerValue);
    }

    /**
     * Removes the specified header from the response.
     *
     * @param headerName The header name.
     */
    public void removeHeader(String headerName) {
        getEntityWithoutBody().removeHeader(headerName);
    }

    /**
     * Removes all the headers from the response.
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
     * Sets the `content-type` header to the response.
     *
     * @param contentType Content type value to be set as the `content-type` header.
     */
    public void setContentType(String contentType) {
        setHeader(Constants.CONTENT_TYPE_HEADER, contentType);

    }

    /**
     * Gets the type of the payload of the response (i.e: the `content-type` header value).
     *
     * @return Returns the `content-type` header value as a string. If not present returns empty string
     */
    public String getContentType() {
        return hasHeader(Constants.CONTENT_TYPE_HEADER) ? getHeader(Constants.CONTENT_TYPE_HEADER) : "";
    }

    /**
     * Extracts `json` payload from the response. If the content type is not JSON, an exception will be thrown.
     *
     * @return The `json` {@link JSONObject} payload of the request. Null if json body is not found in the response.
     * @throws InterceptorException If error while getting json payload.
     */
    public JSONObject getJsonPayload() throws InterceptorException {
        return getEntity().getJson();
    }

    /**
     * Extracts `json array` payload from the response. If the content type is not JSON, an exception will be thrown.
     *
     * @return The `json` {@link JSONArray} payload of the request. Null if json body is not found in the response.
     * @throws InterceptorException If error while getting json array payload.
     */
    public JSONArray getJsonArrayPayload() throws InterceptorException {
        return getEntity().getJsonArray();
    }

    /**
     * Extracts `xml` payload from the response. If the content type is not XML, an exception will be thrown.
     *
     * @return {@link BXML} The `xml` payload of the response.
     * @throws InterceptorException If error while getting xml payload.
     */
    public BXML getXmlPayload() throws InterceptorException {
        return getEntity().getXml();
    }

    /**
     * Extracts `text` payload from the response. If the content type is not text, an exception will be thrown.
     *
     * @return The `text` payload of the response.
     * @throws InterceptorException If error while getting text payload.
     */
    public String getTextPayload() throws InterceptorException {
        return getEntity().getText();
    }

    /**
     * Gets the response payload as a `ByteChannel` except in the case of multiparts. To retrieve multiparts, use
     * `Request.getBodyParts()`.
     *
     * @return {@link ByteChannel} A byte channel from which the
     * message payload can be read.
     * @throws InterceptorException If error while getting byte channel of the request.
     */
    public ByteChannel getByteChannel() throws InterceptorException {
        return getEntity().getByteChannel();
    }

    /**
     * Gets the response payload as a `byte[]`.
     *
     * @return The byte[] representation of the message payload
     * @throws InterceptorException If error while getting byte array of the request.
     */
    public byte[] getBinaryPayload() throws InterceptorException {
        return getEntity().getByteArray();
    }

    /**
     * Extracts body parts from the response. If the content type is not a composite media type, an exception
     * is thrown.
     *
     * @return Returns the body parts as an array of entities.
     * @throws InterceptorException if there were any errors constructing the body parts from the reponse.
     */
    public Entity[] getBodyParts() throws InterceptorException {
        return getEntity().getBodyParts();
    }

    /**
     * Sets a string text content as the payload.
     *
     * @param textPayload The text payload.
     */
    public void setTextPayload(String textPayload) {
        getEntity().setText(textPayload);
        setEntity(entity);
    }

    /**
     * Sets a byte[]  content as the payload.
     *
     * @param binaryPayload The byte[] payload.
     */
    public void setBinaryPayload(byte[] binaryPayload) {
        getEntity().setBinary(binaryPayload);
        setEntity(entity);
    }

    /**
     * Sets a json as the payload.
     *
     * @param jsonPayload {@link JSONObject} The json payload.
     */
    public void setJsonPayload(JSONObject jsonPayload) {
        getEntity().setJson(jsonPayload);
        setEntity(entity);
    }

    /**
     * Sets a json array as the payload.
     *
     * @param jsonArrayPayload {@link JSONArray} The json payload.
     */
    public void setJsonPayload(JSONArray jsonArrayPayload) {
        getEntity().setJson(jsonArrayPayload);
        setEntity(entity);
    }

    /**
     * Sets a xml as the payload.
     *
     * @param xmlPayload The xml {@link BXML} payload.
     */
    public void setXmlPayload(BXML xmlPayload) {
        getEntity().setXml(xmlPayload);
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
     * Returns the java native object of the ballerina level http:Response object.
     *
     * @return Native ballerina object {@link ObjectValue} representing the response.
     */
    public ObjectValue getNativeRequestObject() {
        return responseObj;
    }

    public void setEntity(Entity entity) {
        ExternResponse.setEntity(responseObj, entity.getEntityObj());
    }

    protected ObjectValue getResponseObjectValue() {
        return responseObj;
    }

    public Entity getEntityWithoutBody() {
        entity = new Entity(HttpUtil.getEntity(responseObj, false, false));
        return entity;
    }

    public Entity getEntity() {
        entity = new Entity(HttpUtil.getEntity(responseObj, false, true));
        return entity;
    }
}
