package org.wso2.apimgt.gateway.codegen.config;

import org.wso2.apimgt.gateway.codegen.config.bean.Config;
import org.wso2.apimgt.gateway.codegen.config.bean.ContainerConfig;
import org.wso2.apimgt.gateway.codegen.exception.ConfigParserException;

public class TOMLTest {
    public static void main(String[] args) throws ConfigParserException {
        Config config = TOMLConfigParser.parse("/home/harsha/wso2/apim/repos/product-microgateway/components/micro-gateway-cli/src/main/resources/default-config.toml", Config.class);
        config.getToken().setClientId("BLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLA");
        TOMLConfigParser.write("/home/harsha/wso2/apim/repos/product-microgateway/components/micro-gateway-cli/src/main/resources/default-config.toml", config);

        ContainerConfig containerConfig = TOMLConfigParser.parse("/home/harsha/wso2/apim/repos/product-microgateway/components/micro-gateway-cli/src/main/resources/default-label-config.toml", ContainerConfig.class);

    }
}
