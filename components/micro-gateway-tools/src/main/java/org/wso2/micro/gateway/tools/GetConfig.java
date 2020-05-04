package org.wso2.micro.gateway.tools;

import com.moandjiezana.toml.Toml;
import org.wso2.micro.gateway.tools.model.Config;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Extern function wso2.gateway:getConfigurations.
 */
public class GetConfig {

    /**
     * reading the configurations from micro-gw.conf.
     *
     * @param confFilePath Path of the micro-gw conf file
     * @param fileWritePath File path of the txt file
     * @throws IOException exception if an error occurs when compressing
     */
    public void getConfigurations(String confFilePath, String fileWritePath) throws IOException {
        File configFile = new File (confFilePath);
        Toml toml = new Toml();
        toml.read(configFile);
        Config config = toml.to(Config.class);
        boolean enabled = false;
        long jmxPort;
        if (config.getB7a() != null && config.getB7a().getObservability() != null &&
                config.getB7a().getObservability().getMetrics() != null) {
            enabled = config.getB7a().getObservability().getMetrics().isEnabled();
            jmxPort = config.getB7a().getObservability().getMetrics().getPrometheus().getJmxPort();
            File txtConfigs = new File(fileWritePath);
            try (PrintWriter writer = new PrintWriter(txtConfigs, "UTF-8")) {
                writer.println(enabled);
                writer.println(jmxPort);
            }
        }
    }

}
