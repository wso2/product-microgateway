package org.wso2.micro.gateway.tests.throttling;


import org.wso2.micro.gateway.tests.context.Constants;

import java.io.File;

/**
 * Datapublisher Test Util.
 */
public class DataPublisherTestUtil {
    public static final String LOCAL_HOST = "localhost";

    public static void setKeyStoreParams() {
        File filePath = new File(DataPublisherTestUtil.class.getClassLoader().getResource("distributedThrottling/"
                + "wso2carbon.jks").getPath());
        String keyStore = filePath.getAbsolutePath();
        System.setProperty("Security.KeyStore.Location", keyStore);
        System.setProperty("Security.KeyStore.Password", "wso2carbon");

        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
    }

    public static String getDataBridgeConfigPath(String configFileName) {
        File filePath = new File(DataPublisherTestUtil.class.getClassLoader().getResource("distributedThrottling/"
                + configFileName).getPath());
        return filePath.getAbsolutePath();
    }

}
