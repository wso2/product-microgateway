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

package org.wso2.micro.gateway.core.interceptors;

import org.ballerinalang.jvm.values.ArrayValue;
import org.ballerinalang.jvm.values.ArrayValueImpl;
import org.ballerinalang.jvm.values.ErrorValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.mime.nativeimpl.EntityHeaders;
import org.ballerinalang.mime.nativeimpl.MimeDataSourceBuilder;
import org.ballerinalang.mime.nativeimpl.MimeEntityBody;
import org.ballerinalang.mime.util.MimeConstants;
import org.ballerinalang.net.http.HttpUtil;
import org.ballerinalang.net.http.ValueCreatorUtils;
import org.ballerinalang.net.http.nativeimpl.ExternResponse;
import org.ballerinalang.stdlib.io.channels.base.Channel;
import org.ballerinalang.stdlib.io.utils.IOConstants;

/**
 * Representation of ballerina http:Response object. Provide methods to do CRUD operations on the response object
 * when writing gateway interceptors.
 */
public class Response {
    private ObjectValue responseObj;

    public Response(ObjectValue responseObj) {
        this.responseObj = responseObj;
    }

    public Response() {
        responseObj = ValueCreatorUtils.createResponseObject();
        ExternResponse.createNewEntity(responseObj);
    }

    /**
     * Checks whether the requested header key exists in the header map.
     *
     * @param headerName The header name.
     * @return Returns true if the specified header key exists
     */
    public boolean hasHeader(String headerName) {
        ObjectValue entity = HttpUtil.getEntity(responseObj, false, false);
        return EntityHeaders.hasHeader(entity, headerName, Constants.LEADING_HEADER);
    }

    /**
     * Returns the value of the specified header. If the specified header key maps to multiple values, the first of
     * these values is returned.
     *
     * @param headerName The header name.
     * @return The first header value for the specified header name. Null is returned if header does not present.
     */
    public String getHeader(String headerName) {
        if (hasHeader(headerName)) {
            ObjectValue entity = HttpUtil.getEntity(responseObj, false, false);
            return EntityHeaders.getHeader(entity, headerName, Constants.LEADING_HEADER);
        }
        return null;
    }

    /**
     * Adds the specified header to the response. Existing header values are not replaced.
     *
     * @param headerName  The header name.
     * @param headerValue The header value.
     */
    public void addHeader(String headerName, String headerValue) {
        ObjectValue entity = HttpUtil.getEntity(responseObj, false, false); // check getEntityIfAvailable can be used.
        EntityHeaders.addHeader(entity, headerName, headerValue, Constants.LEADING_HEADER);

    }

    /**
     * Gets all the header values to which the specified header key maps to.
     *
     * @param headerName The header name.
     * @return The header values the specified header key maps to. Null is returned if header does not present.
     */
    public String[] getHeaders(String headerName) {
        if (hasHeader(headerName)) {
            ObjectValue entity = HttpUtil.getEntity(responseObj, false, false);
            ArrayValue headerArray = EntityHeaders.getHeaders(entity, headerName, Constants.LEADING_HEADER);
            String[] stringArray;
            if ((stringArray = headerArray.getStringArray()) != null) {
                return stringArray;
            }
        }
        return null;
    }

    /**
     * Sets the specified header to the response. If a mapping already exists for the specified header key, the existing
     * header value is replaced with the specified header value.
     *
     * @param headerName  The header name.
     * @param headerValue The header value.
     */
    public void setHeader(String headerName, String headerValue) {
        ObjectValue entity = HttpUtil.getEntity(responseObj, false, false);
        EntityHeaders.setHeader(entity, headerName, headerValue, Constants.LEADING_HEADER);
    }

    /**
     * Removes the specified header from the response.
     *
     * @param headerName The header name.
     */
    public void removeHeader(String headerName) {
        ObjectValue entity = HttpUtil.getEntity(responseObj, false, false);
        EntityHeaders.removeHeader(entity, headerName, Constants.LEADING_HEADER);
    }

    /**
     * Removes all the headers from the response.
     */
    public void removeAllHeaders() {
        ObjectValue entity = HttpUtil.getEntity(responseObj, false, false);
        EntityHeaders.removeAllHeaders(entity, Constants.LEADING_HEADER);
    }

    /**
     * Gets all the names of the headers of the request.
     *
     * @return An array of all the header names.
     */
    public String[] getHeaderNames() {
        ObjectValue entity = HttpUtil.getEntity(responseObj, false, false);
        ArrayValue headerNames = EntityHeaders.getHeaderNames(entity, Constants.LEADING_HEADER);
        return headerNames.getStringArray();
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
     * Extracts `text` payload from the response. If the content type is not text, an exception will be thrown.
     *
     * @return The `text` payload of the request.
     * @throws InterceptorException If error while getting text payload.
     */
    public String getTextPayload() throws InterceptorException {
        ObjectValue entity = HttpUtil.getEntity(responseObj, false, true);
        Object textPayload = MimeDataSourceBuilder.getText(entity);
        if (textPayload instanceof ErrorValue) {
            throw new InterceptorException("Error while getting text payload from the response",
                    ((ErrorValue) textPayload).getCause());
        }
        return textPayload.toString();
    }

    /**
     * Gets the response payload as a `ByteChannel` except in the case of multiparts. To retrieve multiparts, use
     * `Request.getBodyParts()`.
     *
     * @return {@link Channel} A byte channel from which the message payload can be read.
     * @throws InterceptorException If error while getting byte channel of the request.
     */
    public Channel getByteChannel() throws InterceptorException {
        ObjectValue entity = HttpUtil.getEntity(responseObj, false, true);
        Object byteChannel = MimeEntityBody.getByteChannel(entity);
        if (byteChannel instanceof ErrorValue) {
            throw new InterceptorException("Error while getting byte channel from the response",
                    ((ErrorValue) byteChannel).getCause());
        }
        ObjectValue byteChannelObject = (ObjectValue) byteChannel;
        return ((Channel) byteChannelObject.getNativeData(IOConstants.BYTE_CHANNEL_NAME));
    }

    /**
     * Gets the response payload as a `byte[]`.
     *
     * @return The byte[] representation of the message payload
     * @throws InterceptorException If error while getting byte array of the request.
     */
    public byte[] getBinaryPayload() throws InterceptorException {
        ObjectValue entity = HttpUtil.getEntity(responseObj, false, true);
        Object binaryPayload = MimeDataSourceBuilder.getByteArray(entity);
        if (binaryPayload instanceof ErrorValue) {
            throw new InterceptorException("Error while getting byte array from the request",
                    ((ErrorValue) binaryPayload).getCause());
        }
        ArrayValue byteArray = (ArrayValue) binaryPayload;
        return byteArray.getBytes();
    }

    /**
     * Sets a string text content as the payload.
     *
     * @param textPayload The text payload.
     */
    public void setTextPayload(String textPayload) {
        ObjectValue entity = HttpUtil.getEntity(responseObj, false, false);
        MimeEntityBody.setText(entity, textPayload, MimeConstants.TEXT_PLAIN);
    }

    /**
     * Sets a byte[]  content as the payload.
     *
     * @param binaryPayload The byte[] payload.
     */
    public void setBinaryPayload(byte[] binaryPayload) {
        ObjectValue entity = HttpUtil.getEntity(responseObj, false, false);
        MimeEntityBody.setByteArray(entity, new ArrayValueImpl(binaryPayload), MimeConstants.OCTET_STREAM);
    }

    /**
     * Sets a json string as the payload.
     *
     * @param jsonPayload The json payload.
     */
    public void setJsonPayload(String jsonPayload) {
        ObjectValue entity = HttpUtil.getEntity(responseObj, false, false);
        MimeEntityBody.setJson(entity, jsonPayload, MimeConstants.APPLICATION_JSON);
    }

    protected ObjectValue getResponseObjectValue() {
        return responseObj;
    }
}
