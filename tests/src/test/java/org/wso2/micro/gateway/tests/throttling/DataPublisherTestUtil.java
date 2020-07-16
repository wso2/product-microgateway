package org.wso2.micro.gateway.tests.throttling;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.config.ConfigProviderFactory;
import org.wso2.carbon.config.ConfigurationException;
import org.wso2.carbon.config.provider.ConfigProvider;
import org.wso2.carbon.databridge.core.conf.DataBridgeConfiguration;
import org.wso2.carbon.databridge.core.conf.DatabridgeConfigurationFileResolver;
import org.wso2.carbon.databridge.core.exception.DataBridgeConfigurationException;
import org.wso2.micro.gateway.tests.context.Constants;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;

/**
 * Datapublisher Test Util.
 */
public class DataPublisherTestUtil {
    public static final String LOCAL_HOST = "localhost";
    public static String keyStorePath = null;
    public static String keyStorePassword = null;
    public static Logger log = LoggerFactory.getLogger(DataPublisherTestUtil.class);

    public static void setKeyStoreParams() {
        File filePath = new File(DataPublisherTestUtil.class.getClassLoader().getResource("distributedThrottling/"
                + "wso2carbon.jks").getPath());
        String keyStore = filePath.getAbsolutePath();
        keyStorePath = keyStore;
        keyStorePassword = "wso2carbon";
    }
}
