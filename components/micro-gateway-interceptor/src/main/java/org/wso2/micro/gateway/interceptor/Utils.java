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

