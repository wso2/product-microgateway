package org.wso2.apimgt.gateway.codegen.token;

import org.wso2.apimgt.gateway.codegen.config.bean.Config;

public interface TokenManagement {

    String generateAccessToken(String username, char[] password, String clientId, char[] clientSecret);

    String generateClientIdAndSecret(Config config, String root);
}
