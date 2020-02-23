package org.wso2.micro.gateway.interceptor;

import org.ballerinalang.jvm.values.api.BMap;
import org.ballerinalang.stdlib.runtime.nativeimpl.GetInvocationContext;

/**
 * Utility methods in order to write java level interceptors.
 */
public class Utils {

    /**
     * Provided the invocation context object. This object can be used to share the information between the request
     * path and the response path. Same context is available in both request and response path.
     *
     * @return The invocation context object as a  {@link BMap}.
     */
    public static BMap getInvocationContext() {
        return GetInvocationContext.getInvocationContext();
    }
}

