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

/**
 * Representation of ballerina http:Caller object. This caller object can be used extract the dat about the client
 * and also to respond to a client with a http response.
 */
public class Caller {
    private ObjectValue callerObj;

    public Caller(ObjectValue callerObj) {
        this.callerObj = callerObj;
    }

    /**
     * Method use to send the response to the client. If used from either request or response interceptor
     * set the return value as false. Then this will stop sending multiple responses to the client.
     * If used in request interceptor then set the return value of method
     * {@link Interceptor#interceptRequest(Caller, Request)} as false.
     * If used in response interceptor then set the return value of method
     * {@link Interceptor#interceptResponse(Caller, Response)} as false
     */
    public void respond(Response response) {
        Utils.addDataToContextAttributes(Constants.RESPOND_DONE, true);
        Utils.addDataToContextAttributes(Constants.RESPONSE_OBJECT, response.getResponseObjectValue());
    }

    /**
     * Returns the java native object of the ballerina level http:Caller object.
     *
     * @return Native ballerina object {@link ObjectValue} representing the caller.
     */
    public ObjectValue getNativeRequestObject() {
        return callerObj;
    }

    /**
     * Get the local address from the caller object.
     *
     * @return local address as a string in the format host:port
     */
    public String getLocalAddress() {
        MapValue obj = (MapValue) callerObj.get(Constants.LOCAL_ADDRESS);
        return obj.getStringValue(Constants.HOST) + ":" + obj.getIntValue(Constants.PORT);
    }

    /**
     * Get the remote address from the caller object.
     *
     * @return remote address as a string in the format host:port
     */
    public String getRemoteAddress() {
        MapValue obj = (MapValue) callerObj.get(Constants.REMOTE_ADDRESS);
        return obj.getStringValue(Constants.HOST) + ":" + obj.getIntValue(Constants.PORT);
    }
}
