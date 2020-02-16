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

/**
 * Defines the interface for writing the gateway interceptors.
 */
public interface Interceptor {

    /**
     * Intercepts the request before forwarding the request to the back end.
     *
     * @param caller {@link Caller} The caller object.
     * @param request {@link Request} The request object.
     * @return Returns whether the interceptor has completed the task completely. In order to stop the request
     * flow from the interceptor return false.
     */
    boolean interceptRequest(Caller caller, Request request);

    /**
     * Intercepts the response before forwarding the response to the client.
     *
     * @param caller {@link Caller} The caller object.
     * @param response {@link Response} The response object.
     * @return Returns whether the interceptor has completed the task completely. In order to stop the request
     * flow from the interceptor return false.
     */
    boolean interceptResponse(Caller caller, Response response);
}
