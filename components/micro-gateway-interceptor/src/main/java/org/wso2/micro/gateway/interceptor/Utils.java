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
import org.ballerinalang.jvm.values.api.BMap;
import org.ballerinalang.stdlib.runtime.nativeimpl.GetInvocationContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility methods in order to write java level interceptors.
 */
public class Utils {

    /**
     * Provided the invocation context object. This object can be used to share the information between the request
     * path and the response path. Same context is available in both request and response path.
     *
     * @return The invocation context object as a  {@link Map}.
     */
    public static Map<String, Object> getInvocationContext() {
        return convertMapValueToMap(GetInvocationContext.getInvocationContext());
    }

    /**
     * Provided the invocation context set of attributes. This attributes can be used to
     * share the information between the request path and the response path.
     * Same context is available in both request and response path.
     *
     * @return The invocation context object as a  {@link Map}.
     */
    public static Map<String, Object> getInvocationContextAttributes() {
        return convertMapValueToMap((MapValue<String, Object>) getInvocationContext().get("attributes"));
    }

    /**
     * Add the data to invocation context attributes map as key, value pairs.
     *
     * @param key The string type key used in the attribute map.
     * @param value The value to be inserted in to the attribute map.
     */
    public static void addDataToContextAttributes(String key, Object value) {
        BMap attributesMap = (BMap) GetInvocationContext.getInvocationContext().get("attributes");
        attributesMap.put(key, value);
    }


    private static Map<String, Object> convertMapValueToMap(MapValue<String, Object> mapValue) {
        Map<String, Object> convertedMap = new HashMap<>();
        for (String key : mapValue.getKeys()) {
            convertedMap.put(key, mapValue.get(key));
        }
        return convertedMap;
    }

}
