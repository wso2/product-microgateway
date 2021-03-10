package org.wso2.micro.gateway.enforcer.analytics;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.wso2.micro.gateway.enforcer.api.RequestContext;
import org.wso2.micro.gateway.enforcer.security.AuthenticationContext;

/**
 * Common Utility functions
 */
public class AnalyticsUtils {
    private static final String DEFAULT_FOR_UNASSIGNED = "UnAssigned";

    public static String getAPIId(RequestContext requestContext) {
        AuthenticationContext authContext = requestContext.getAuthenticationContext();
        if (authContext == null || StringUtils.isEmpty(authContext.getApiUUID())) {
            return generateHash(requestContext.getMathedAPI().getAPIConfig().getName(),
                    requestContext.getMathedAPI().getAPIConfig().getVersion());
        }
        return authContext.getApiUUID();
    }

    private static String generateHash(String apiName, String apiVersion) {
        return DigestUtils.md5Hex(apiName + ":" + apiVersion);
    }

    public static String setDefaultIfNull(String value) {
        return value == null ? DEFAULT_FOR_UNASSIGNED : value;
    }

    public static AuthenticationContext getAuthenticationContext(RequestContext requestContext) {
        AuthenticationContext authContext = requestContext.getAuthenticationContext();
        // TODO: (VirajSalaka) Handle properly
        // When authentication failure happens authContext remains null
        if (authContext == null) {
            authContext = new AuthenticationContext();
            authContext.setAuthenticated(false);
        }
        return authContext;
    }
}
