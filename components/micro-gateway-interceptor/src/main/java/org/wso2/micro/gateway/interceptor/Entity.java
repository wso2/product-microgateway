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

import org.ballerinalang.jvm.BallerinaValues;
import org.ballerinalang.jvm.JSONParser;
import org.ballerinalang.jvm.scheduling.Scheduler;
import org.ballerinalang.jvm.types.BArrayType;
import org.ballerinalang.jvm.types.BTypes;
import org.ballerinalang.jvm.values.ArrayValue;
import org.ballerinalang.jvm.values.ErrorValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.jvm.values.XMLValue;
import org.ballerinalang.jvm.values.api.BValueCreator;
import org.ballerinalang.jvm.values.api.BXML;
import org.ballerinalang.mime.nativeimpl.AbstractGetPayloadHandler;
import org.ballerinalang.mime.nativeimpl.EntityHeaders;
import org.ballerinalang.mime.nativeimpl.MimeEntityBody;
import org.ballerinalang.mime.util.MimeConstants;
import org.ballerinalang.mime.util.MimeUtil;
import org.ballerinalang.stdlib.io.channels.base.Channel;
import org.ballerinalang.stdlib.io.utils.IOConstants;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.ByteChannel;

/**
 * Represents the headers and body of a message. This can be used to represent both the entity of a top level message
 * and an entity(body part) inside of a multipart entity.
 */
public class Entity {

    private static final Logger log = LoggerFactory.getLogger("ballerina");
    private ObjectValue entityObj;

    public Entity(ObjectValue entity) {
        this.entityObj = entity;
    }

    public Entity() {
        this.entityObj = BallerinaValues
                .createObjectValue(MimeConstants.PROTOCOL_MIME_PKG_ID, MimeConstants.ENTITY, new Object());
    }

    /**
     * Checks whether the requested header key exists in the header map.
     *
     * @param headerName The header name.
     * @return Returns true if the specified header key exists
     */
    public boolean hasHeader(String headerName) {
        return EntityHeaders.hasHeader(entityObj, headerName, Constants.LEADING_HEADER);
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
            return EntityHeaders.getHeader(entityObj, headerName, Constants.LEADING_HEADER);
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
            ArrayValue headerArray = EntityHeaders.getHeaders(entityObj, headerName, Constants.LEADING_HEADER);
            String[] stringArray;
            if ((stringArray = headerArray.getStringArray()) != null) {
                return stringArray;
            }
        }
        return null;
    }

    /**
     * Sets the specified header to the entity. If a mapping already exists for the specified header key, the existing
     * header value is replaced with the specified header value.
     *
     * @param headerName  The header name.
     * @param headerValue The header value.
     */
    public void setHeader(String headerName, String headerValue) {
        EntityHeaders.setHeader(entityObj, headerName, headerValue, Constants.LEADING_HEADER);
    }

    /**
     * Adds the specified header to the entity. Existing header values are not replaced.
     *
     * @param headerName  The header name.
     * @param headerValue The header value.
     */
    public void addHeader(String headerName, String headerValue) {
        EntityHeaders.addHeader(entityObj, headerName, headerValue, Constants.LEADING_HEADER);

    }

    /**
     * Removes the specified header from the entity.
     *
     * @param headerName The header name.
     */
    public void removeHeader(String headerName) {
        EntityHeaders.removeHeader(entityObj, headerName, Constants.LEADING_HEADER);
    }

    /**
     * Removes all the headers from the entity.
     */
    public void removeAllHeaders() {
        EntityHeaders.removeAllHeaders(entityObj, Constants.LEADING_HEADER);
    }

    /**
     * Gets all the names of the headers of the entity. If headers are not present returns an empty array.
     *
     * @return An array of all the header names.
     */
    public String[] getHeaderNames() {
        ArrayValue headerNames = EntityHeaders.getHeaderNames(entityObj, Constants.LEADING_HEADER);

        if (headerNames.getValues() != null) {
            Object[] headerObjects = headerNames.getValues();
            String[] headers = new String[headerObjects.length];
            for (int index = 0; index < headerObjects.length; index++) {
                headers[index] = headerObjects[index].toString();
            }
            return headers;
        }
        return new String[0];
    }

    /**
     * Extracts `json` payload from the entity. If the content type is not JSON, an exception will be thrown.
     *
     * @return The `json` payload of the request.
     * @throws InterceptorException If error while getting json payload.
     */
    public JSONObject getJson() throws InterceptorException {
        String jsonObject = getJsonStringPayload();
        if (jsonObject != null) {
            return new JSONObject(jsonObject);
        }
        return null;
    }

    /**
     * Extracts `json array` payload from the entity. If the content type is not JSON, an exception will be thrown.
     *
     * @return The `json array` payload of the request.
     * @throws InterceptorException If error while getting json payload.
     */
    public JSONArray getJsonArray() throws InterceptorException {
        String jsonObject = getJsonStringPayload();
        if (jsonObject != null) {
            return new JSONArray(jsonObject);
        }
        return null;
    }

    /**
     * Extracts `xml` payload from the entity. If the content type is not XML, an exception will be thrown.
     *
     * @return {@link BXML} The `xml` payload of the request.
     * @throws InterceptorException If error while getting xml payload.
     */
    public BXML getXml() throws InterceptorException {
        constructPayloadBlockingCallBack(AbstractGetPayloadHandler.SourceType.XML);
        if (entityObj.getNativeData(MimeConstants.MESSAGE_DATA_SOURCE) != null) {
            return (BXML) entityObj.getNativeData(MimeConstants.MESSAGE_DATA_SOURCE);
        }
        return null;
    }

    /**
     * Extracts `text` payload from the entity. If the content type is not text, an exception will be thrown.
     *
     * @return The `text` payload of the request.
     * @throws InterceptorException If error while getting text payload.
     */
    public String getText() throws InterceptorException {
        constructPayloadBlockingCallBack(AbstractGetPayloadHandler.SourceType.TEXT);
        if (entityObj.getNativeData(MimeConstants.MESSAGE_DATA_SOURCE) != null) {
            return  entityObj.getNativeData(MimeConstants.MESSAGE_DATA_SOURCE).toString();
        }
        return null;
    }

    /**
     * Given an entity, gets the entity body as a byte channel
     *
     * @return {@link ByteChannel} A byte channel from which the message payload can be read.
     * @throws InterceptorException If error while getting byte channel of the entity.
     */
    public ByteChannel getByteChannel() throws InterceptorException {
        Object byteChannel = MimeEntityBody.getByteChannel(entityObj);
        if (byteChannel instanceof ErrorValue) {
            throw new InterceptorException("Error while getting byte channel from the entity",
                    ((ErrorValue) byteChannel).getCause());
        }
        ObjectValue byteChannelObject = (ObjectValue) byteChannel;
        Channel channel = ((Channel) byteChannelObject.getNativeData(IOConstants.BYTE_CHANNEL_NAME));
        return channel.getByteChannel();
    }

    /**
     * Gets the entity payload as a `byte[]`.
     *
     * @return The byte[] representation of the message payload
     * @throws InterceptorException If error while getting byte array of the entity.
     */
    public byte[] getByteArray() throws InterceptorException {
        constructPayloadBlockingCallBack(AbstractGetPayloadHandler.SourceType.BLOB);
        if (entityObj.getNativeData(MimeConstants.MESSAGE_DATA_SOURCE) != null) {
            return (byte[]) entityObj.getNativeData(MimeConstants.MESSAGE_DATA_SOURCE);
        }
        return null;
    }

    /**
     * Given an entity, gets its body parts. If the entity body is not a set of body parts an exception will be thrown.
     *
     * @return An array of body parts{@link Entity[]} extracted from the entity body
     * @throws InterceptorException If error while getting byte array of the entity.
     */
    public Entity[] getBodyParts() throws InterceptorException {
        Object arrayValue = MimeEntityBody.getBodyParts(entityObj);
        if (arrayValue instanceof ErrorValue) {
            throw new InterceptorException("Error while getting byte array from the request",
                    ((ErrorValue) arrayValue).getCause());
        }
        Object[] entityObjects = ((ArrayValue) arrayValue).getValues();
        Entity[] entities = new Entity[entityObjects.length];
        for (int index = 0; index < entityObjects.length; index++) {
            entities[index] = new Entity((ObjectValue) entityObjects[index]);
        }
        return entities;
    }

    /**
     * Sets a json {@link JSONObject} as the payload to the entity.
     *
     * @param jsonPayload {@link JSONObject} The json payload.
     */
    public void setJson(JSONObject jsonPayload) {
        MimeEntityBody.setJson(entityObj, JSONParser.parse(jsonPayload.toString()), MimeConstants.APPLICATION_JSON);
    }

    /**
     * Sets a json array{@link JSONArray} as the payload to the entity.
     *
     * @param jsonArrayPayload {@link JSONArray} The json payload.
     */
    public void setJson(JSONArray jsonArrayPayload) {
        MimeEntityBody
                .setJson(entityObj, JSONParser.parse(jsonArrayPayload.toString()), MimeConstants.APPLICATION_JSON);
    }

    /**
     * Sets a xml to the entity.
     *
     * @param xmlPayload The xml {@link BXML} payload.
     */
    public void setXml(BXML xmlPayload) {
        MimeEntityBody.setXml(entityObj, (XMLValue) xmlPayload, MimeConstants.APPLICATION_XML);
    }

    /**
     * Sets a string text content to the entity.
     *
     * @param textPayload The text payload.
     */
    public void setText(String textPayload) {
        MimeEntityBody.setText(entityObj, textPayload, MimeConstants.TEXT_PLAIN);
    }

    /**
     * Sets a byte[] content to the entity.
     *
     * @param binaryPayload The byte[] payload.
     */
    public void setBinary(byte[] binaryPayload) {
        MimeEntityBody.setByteArray(entityObj, (ArrayValue) BValueCreator.createArrayValue(binaryPayload),
                MimeConstants.OCTET_STREAM);
    }

    /**
     * Set multiparts as the payload.
     *
     * @param bodyParts   The entities which make up the message body
     * @param contentType The content type of the top level message. Set this to override the default
     *                    `content-type` header value which is 'multipart/form-data'
     */
    public void setBodyParts(Entity[] bodyParts, String contentType) {
        if (contentType == null) {
            contentType = Constants.MULTIPART_FORM_DATA;
        }
        ObjectValue[] entityObjects = new ObjectValue[bodyParts.length];
        for (int index = 0; index < bodyParts.length; index++) {
            entityObjects[index] = bodyParts[index].getEntityObj();
        }
        ArrayValue arrValues = (ArrayValue) BValueCreator
                .createArrayValue(entityObjects, new BArrayType(BTypes.typeAny));
        MimeEntityBody.setBodyParts(entityObj, arrValues, contentType);
    }

    /**
     * Sets a `ByteChannel`  to the entity.
     *
     * @param channel - A `ByteChannel` {@link Channel} through which the message payload can be read
     * @param contentType The content type of the top level message. Set this to override the default
     *                    `content-type` header value which is 'application/octet-stream'
     */
    public void setByteChannel(Channel channel, String contentType) {
        if (contentType == null) {
            contentType = Constants.OCTET_STREAM;
        }
        ObjectValue channelObj = BallerinaValues
                .createObjectValue(IOConstants.IO_PACKAGE_ID, Constants.READABLE_BYTE_CHANNEL);
        channelObj.addNativeData(IOConstants.BYTE_CHANNEL_NAME, channel);
        MimeEntityBody.setByteChannel(entityObj, channelObj, contentType);
    }

    /**
     * Returns the java native object of the ballerina level mime:Entity object.
     *
     * @return Native ballerina object {@link ObjectValue} representing the entity.
     */
    public ObjectValue getEntityObj() {
        return entityObj;
    }

    private void constructPayloadBlockingCallBack(AbstractGetPayloadHandler.SourceType type)
            throws InterceptorException {
        MGWBlockingCallBack callback = new MGWBlockingCallBack(Scheduler.getStrand());
        try {
            AbstractGetPayloadHandler.constructNonBlockingDataSource(callback, entityObj, type);
            callback.sync();
        } catch (Exception e) {
            // we need to catch all the exceptions including runtime thrown from ballerina side in order to avoid the
            // call back syncing process.
            String msg = "Error while getting the payload with type : " + type.toString();
            log.error(msg, e);
            callback.notifyFailure();
            throw new InterceptorException(msg, e);
        }
    }

    private String getJsonStringPayload() throws InterceptorException {
        constructPayloadBlockingCallBack(AbstractGetPayloadHandler.SourceType.JSON);
        if (entityObj.getNativeData(MimeConstants.MESSAGE_DATA_SOURCE) != null) {
            return MimeUtil
                    .getMessageAsString(entityObj.getNativeData(MimeConstants.MESSAGE_DATA_SOURCE));
        }
        return null;
    }

}
