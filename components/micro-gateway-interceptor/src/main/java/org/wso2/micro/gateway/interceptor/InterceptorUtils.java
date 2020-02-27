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
        for (Object key : bMap.getKeys()) {
            convertedMap.put(key.toString(), bMap.get(key).toString());
        }
        return convertedMap;
    }
}
