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

import org.ballerinalang.jvm.values.api.BMap;

import java.util.HashMap;
import java.util.Map;

/**
 * These are internal utility methods used by the micro gateway java interceptor library.
 */
public class InterceptorUtils {

    protected static Map<String, String> convertBMapToMap(BMap bMap) {
        Map<String, String> convertedMap = new HashMap<>();
        if (bMap != null) {
            for (Object key : bMap.getKeys()) {
                convertedMap.put(key.toString(), bMap.get(key).toString());
            }
        }
        return convertedMap;
    }
}
