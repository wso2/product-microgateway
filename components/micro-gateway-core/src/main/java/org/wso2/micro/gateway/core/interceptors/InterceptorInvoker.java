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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ballerinalang.jvm.scheduling.Scheduler;
import org.ballerinalang.jvm.values.ObjectValue;
import org.wso2.micro.gateway.interceptor.Caller;
import org.wso2.micro.gateway.interceptor.Interceptor;
import org.wso2.micro.gateway.interceptor.Request;
import org.wso2.micro.gateway.interceptor.Response;

/**
 * Class to dynamically invoke the interceptors defined.
 */
public class InterceptorInvoker {
    private static Interceptor[] interceptorArray;
    private static int index = 0;
    private static final Logger log = LogManager.getLogger(InterceptorInvoker.class);

    public static void initiateInterceptorArray(int arraySize) {
        interceptorArray = new Interceptor[arraySize];
    }

    public static int loadInterceptorClass(String className) {
        try {
            Class interceptorClass = InterceptorInvoker.class.getClassLoader().loadClass(className);
            Interceptor interceptor = (Interceptor) interceptorClass.newInstance();
            int returnIndex = index;
            interceptorArray[returnIndex] = interceptor;
            index++;
            return returnIndex;
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            log.error("Error while loading the interceptor class: " + className, e);
        }
        return -1;
    }

    public static boolean invokeRequestInterceptor(int arrayIndex, ObjectValue caller, ObjectValue request) {
        boolean returnedValue = interceptorArray[arrayIndex]
                .interceptRequest(new Caller(caller), new Request(request));
        Scheduler.getStrand().setReturnValues(returnedValue);
        return returnedValue;
    }

    public static boolean invokeResponseInterceptor(int arrayIndex, ObjectValue caller, ObjectValue response) {
        boolean returnedValue = interceptorArray[arrayIndex]
                .interceptResponse(new Caller(caller), new Response(response));
        Scheduler.getStrand().setReturnValues(returnedValue);
        return returnedValue;
    }
}
