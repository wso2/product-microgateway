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

import org.ballerinalang.jvm.values.ArrayValue;
import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.jvm.values.api.BArray;
import org.ballerinalang.jvm.values.api.BMap;
import org.ballerinalang.mime.nativeimpl.EntityHeaders;
import org.ballerinalang.net.http.HttpUtil;
import org.ballerinalang.net.http.nativeimpl.ExternRequest;

/**
 * Representation of ballerina http:Request object. Provide methods to do CRUD operations on the request object
 * when writing gateway interceptors.
 */
public class Request {
    private ObjectValue requestObj;
    private ObjectValue entityWithoutBody;

    public Request(ObjectValue requestObj) {
        this.requestObj = requestObj;
    }

    /**
     * Gets the query parameters of the request as a ballerina map.
     *
     * @return {@link BMap} Ballerina map value object containing query parameters.
     */
    public BMap<String, Object> getQueryParams() {
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
        BMap mapValue = getQueryParams();
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
        BMap mapValue = getQueryParams();
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
    public BMap<String, Object> getMatrixParams(String path) {
        return ExternRequest.getMatrixParams(requestObj, path);
    }

    /**
     * Checks whether the requested header key exists in the header map.
     *
     * @param headerName The header name.
     * @return Returns true if the specified header key exists
     */
    public boolean hasHeader(String headerName) {
        ObjectValue entity = HttpUtil.getEntity(requestObj, true, false);
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
            ObjectValue entity = HttpUtil.getEntity(requestObj, true, false);
            return EntityHeaders.getHeader(entity, headerName, Constants.LEADING_HEADER);
        }
        return null;
    }

    /**
     * Gets all the header values to which the specified header key maps to.
     *
     * @param headerName The header name.
     * @return The header values the specified header key maps to. Null is returned if header does not present.
     */
    public String[] getHeaders(String headerName) {
        if (hasHeader(headerName)) {
            ObjectValue entity = HttpUtil.getEntity(requestObj, true, false);
            ArrayValue headerArray = EntityHeaders.getHeaders(entity, headerName, Constants.LEADING_HEADER);
            String[] stringArray;
            if ((stringArray = headerArray.getStringArray()) != null) {
                return stringArray;
            }
        }
        return null;
    }

    /**
     * Sets the specified header to the request. If a mapping already exists for the specified header key, the existing
     * header value is replaced with the specified header value.
     *
     * @param headerName  The header name.
     * @param headerValue The header value.
     */
    public void setHeader(String headerName, String headerValue) {
        ObjectValue entity = HttpUtil.getEntity(requestObj, true, false);
        EntityHeaders.setHeader(entity, headerName, headerValue, Constants.LEADING_HEADER);
    }

    /**
     * Adds the specified header to the request. Existing header values are not replaced.
     *
     * @param headerName  The header name.
     * @param headerValue The header value.
     */
    public void addHeader(String headerName, String headerValue) {
        ObjectValue entity = HttpUtil.getEntity(requestObj, true, false); // check getEntityIfAvailable can be used.
        EntityHeaders.addHeader(entity, headerName, headerValue, Constants.LEADING_HEADER);

    }

    /**
     * Removes the specified header from the request.
     *
     * @param headerName The header name.
     */
    public void removeHeader(String headerName) {
        ObjectValue entity = HttpUtil.getEntity(requestObj, true, false);
        EntityHeaders.removeHeader(entity, headerName, Constants.LEADING_HEADER);
    }

    /**
     * Removes all the headers from the request.
     */
    public void removeAllHeaders() {
        ObjectValue entity = HttpUtil.getEntity(requestObj, true, false);
        EntityHeaders.removeAllHeaders(entity, Constants.LEADING_HEADER);
    }

    /**
     * Gets all the names of the headers of the request.
     *
     * @return An array of all the header names.
     */
    public String[] getHeaderNames() {
        ObjectValue entity = HttpUtil.getEntity(requestObj, true, false);
        ArrayValue headerNames = EntityHeaders.getHeaderNames(entity, Constants.LEADING_HEADER);
        return headerNames.getStringArray();
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

//    /**
//     * Extracts `json` payload from the request. If the content type is not JSON, an exception will be thrown.
//     *
//     * @return The `json` payload of the request.
//     * @throws InterceptorException If error while getting json payload.
//     */
//    public Object getJsonPayload() throws InterceptorException {
//        ObjectValue entity = HttpUtil.getEntity(requestObj, true, true);
//        Object jsonBody = MimeDataSourceBuilder.getJson(entity);
//        if (jsonBody instanceof ErrorValue) {
//            throw new InterceptorException("Error while getting JSON payload from the request",
//                    ((ErrorValue) jsonBody).getCause());
//        }
//        return jsonBody;
//    }

//    /**
//     * Extracts `xml` payload from the request. If the content type is not XML, an exception will be thrown.
//     *
//     * @return {@link XMLValue} The `xml` payload of the request.
//     * @throws InterceptorException If error while getting xml payload.
//     */
//    public XMLValue getXmlPayload() throws InterceptorException {
//        ObjectValue entity = HttpUtil.getEntity(requestObj, true, true);
//        Object xmlBody = MimeDataSourceBuilder.getXml(entity);
//        if (xmlBody instanceof ErrorValue) {
//            throw new InterceptorException("Error while getting XML payload from the request",
//                    ((ErrorValue) xmlBody).getCause());
//        }
//        return (XMLValue) xmlBody;
//
//    }

//    /**
//     * Extracts `text` payload from the request. If the content type is not text, an exception will be thrown.
//     *
//     * @return The `text` payload of the request.
//     * @throws InterceptorException If error while getting text payload.
//     */
//    public String getTextPayload() throws InterceptorException {
//        ObjectValue entity = HttpUtil.getEntity(requestObj, true, true);
//        Object textPayload = MimeDataSourceBuilder.getText(entity);
//        if (textPayload instanceof ErrorValue) {
//            throw new InterceptorException("Error while getting text payload from the request",
//                    ((ErrorValue) textPayload).getCause());
//        }
//        return textPayload.toString();
//    }

//    /**
//     * Gets the request payload as a `ByteChannel` except in the case of multiparts. To retrieve multiparts, use
//     * `Request.getBodyParts()`.
//     *
//     * @return {@link Channel} A byte channel from which the message payload can be read.
//     * @throws InterceptorException If error while getting byte channel of the request.
//     */
//    public Channel getByteChannel() throws InterceptorException {
//        ObjectValue entity = HttpUtil.getEntity(requestObj, true, true);
//        Object byteChannel = MimeEntityBody.getByteChannel(entity);
//        if (byteChannel instanceof ErrorValue) {
//            throw new InterceptorException("Error while getting byte channel from the request",
//                    ((ErrorValue) byteChannel).getCause());
//        }
//        ObjectValue byteChannelObject = (ObjectValue) byteChannel;
//        return ((Channel) byteChannelObject.getNativeData(IOConstants.BYTE_CHANNEL_NAME));
//    }

//    /**
//     * Gets the request payload as a `byte[]`.
//     *
//     * @return The byte[] representation of the message payload
//     * @throws InterceptorException If error while getting byte array of the request.
//     */
//    public byte[] getBinaryPayload() throws InterceptorException {
//        ObjectValue entity = HttpUtil.getEntity(requestObj, true, true);
//        Object binaryPayload = MimeDataSourceBuilder.getByteArray(entity);
//        if (binaryPayload instanceof ErrorValue) {
//            throw new InterceptorException("Error while getting byte array from the request",
//                    ((ErrorValue) binaryPayload).getCause());
//        }
//        ArrayValue byteArray = (ArrayValue) binaryPayload;
//        return byteArray.getBytes();
//    }
//
//    /**
//     * Sets a json string as the payload.
//     *
//     * @param jsonPayload The json payload.
//     */
//    public void setJsonPayload(String jsonPayload) {
//        ObjectValue entity = HttpUtil.getEntity(requestObj, true, false);
//        MimeEntityBody.setJson(entity, jsonPayload, MimeConstants.APPLICATION_JSON);
//    }
//
//    /**
//     * Sets a xml as the payload.
//     *
//     * @param xmlPayload The xml payload.
//     */
//    public void setXmlPayload(XMLValue xmlPayload) {
//        ObjectValue entity = HttpUtil.getEntity(requestObj, true, false);
//        MimeEntityBody.setXml(entity, xmlPayload, MimeConstants.APPLICATION_JSON);
//    }
//
//    /**
//     * Sets a string text content as the payload.
//     *
//     * @param textPayload The text payload.
//     */
//    public void setTextPayload(String textPayload) {
//        ObjectValue entity = HttpUtil.getEntity(requestObj, true, false);
//        MimeEntityBody.setText(entity, textPayload, MimeConstants.TEXT_PLAIN);
//    }
//
//    /**
//     * Sets a byte[]  content as the payload.
//     *
//     * @param binaryPayload The byte[] payload.
//     */
//    public void setBinaryPayload(byte[] binaryPayload) {
//        ObjectValue entity = HttpUtil.getEntity(requestObj, true, false);
//        MimeEntityBody.setByteArray(entity, new ArrayValueImpl(binaryPayload), OCTET_STREAM);
//    }

    private ObjectValue getEntityIfAvailable() {
        if (entityWithoutBody != null) {
            return entityWithoutBody;
        }
        return (entityWithoutBody = HttpUtil.getEntity(requestObj, true, false));
    }

}
