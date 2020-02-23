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
import org.ballerinalang.net.http.nativeimpl.connection.Respond;

/**
 * Representation of ballerina http:Caller object.
 */
public class Caller {
    private ObjectValue callerObj;

    public Caller(ObjectValue callerObj) {
        this.callerObj = callerObj;
    }

    public void respond(Response response) throws InterceptorException {
        Respond.nativeRespond(callerObj, response.getResponseObjectValue());
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
