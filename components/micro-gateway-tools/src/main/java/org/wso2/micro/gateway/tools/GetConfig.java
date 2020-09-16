package org.wso2.micro.gateway.tools;

import com.moandjiezana.toml.Toml;
import org.wso2.micro.gateway.tools.model.Config;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Extern function wso2.gateway:getConfigurations.
 */
public class GetConfig {
    private static final String CONF_JMX_PORT = "jmx_port";
    private static final String CONF_METRICS_ENABLED = "metrics_enabled";

    /**
     * Reading the configurations from micro-gw.conf.
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
            try (PrintWriter writer = new PrintWriter(txtConfigs, StandardCharsets.UTF_8.name())) {
                writer.println(enabled);
                writer.println(jmxPort);
            }
        }
    }

    /**
     * Read configuration from program arguments, env variables and toml configuration file.
     * Then these configuration values will be put into a newline separated file for easier reading from the
     * scripts.
     * @param args program argument list.
     *             0th index should be the path to config file.
     *             1st index should be the path to output file.
     *             rest of the arguments can contain any arbitrary commandline configuration properties
     * @throws IOException When toml config file reading fails
     */
    public void getConfigurations(String[] args) throws IOException {
        String confFilePath = args [0];
        String fileWritePath = args [1];
        Map<String, String> configs = new HashMap<>();

        extractConfigsFromArgs(args, configs);
        if (!isComplete(configs)) {
            extractConfigsFromEnv(configs);
        }

        if (!isComplete(configs)) {
            extractConfigsFromFile(confFilePath, configs);
        }

        if (isComplete(configs)) {
            writeToFile(configs, fileWritePath);
        }
    }

    private void extractConfigsFromFile(String confFilePath, Map<String, String> currentConfigs) {
        File configFile = new File (confFilePath);
        Toml toml = new Toml();
        toml.read(configFile);
        Config config = toml.to(Config.class);
        String isMetricsEnabled;
        String jmxPort;

        if (config.getB7a() != null && config.getB7a().getObservability() != null &&
                config.getB7a().getObservability().getMetrics() != null) {
            isMetricsEnabled = String.valueOf(config.getB7a().getObservability().getMetrics().isEnabled());
            jmxPort = String.valueOf(config.getB7a().getObservability().getMetrics().getPrometheus().getJmxPort());
            currentConfigs.putIfAbsent(CONF_METRICS_ENABLED, isMetricsEnabled);
            currentConfigs.putIfAbsent(CONF_JMX_PORT, jmxPort);
        }
    }

    private void extractConfigsFromEnv(Map<String, String> currentConfigs) {
        final String confMetricsEnabledEnv = "b7a_observability_metrics_enabled";
        final String confJmxPortEnv = "b7a_observability_metrics_prometheus_jmx_port";
        String isMetricsEnabled;
        String jmxPort;
        isMetricsEnabled = System.getenv(confMetricsEnabledEnv);
        jmxPort = System.getenv(confJmxPortEnv);

        if (isMetricsEnabled != null) {
            currentConfigs.putIfAbsent(CONF_METRICS_ENABLED, isMetricsEnabled);
        }
        if (jmxPort != null) {
            currentConfigs.putIfAbsent(CONF_JMX_PORT, jmxPort);
        }
    }

    private void extractConfigsFromArgs(String[] args, Map<String, String> currentConfigs) {
        final String confMetricsEnabledArg = "--b7a.observability.metrics.enabled=";
        final String confJmxPortArg = "--b7a.observability.metrics.prometheus.jmx_port=";
        String isMetricsEnabled = null;
        String jmxPort = null;

        for (String arg: args) {
            // checking variable value to avoid unwanted map lookup
            if (isMetricsEnabled == null) {
                String tmp = getConfigValue(arg, confMetricsEnabledArg);
                if (tmp != null) {
                    isMetricsEnabled = tmp;
                    currentConfigs.put(CONF_METRICS_ENABLED, isMetricsEnabled);
                }
            }
            if (jmxPort == null) {
                String tmp = getConfigValue(arg, confJmxPortArg);
                if (tmp != null) {
                    jmxPort = tmp;
                    currentConfigs.put(CONF_JMX_PORT, jmxPort);
                }
            }

            if (isMetricsEnabled != null && jmxPort != null) {
                break;
            }
        }
    }

    private String getConfigValue (String config, String key) {
        if (config.startsWith(key)) {
            String[] arr = config.split(key);
            if (arr.length == 2) {
                return arr[1];
            }
        }

        return null;
    }

    private void writeToFile(Map<String, String> configs, String filePath) throws IOException {
        File txtConfigs = new File(filePath);
        try (PrintWriter writer = new PrintWriter(txtConfigs, "UTF-8")) {
            // writing configs one by one to preserve required order in the output.
            if (configs.containsKey(CONF_METRICS_ENABLED)) {
                writer.println(configs.get(CONF_METRICS_ENABLED));
            }
            if (configs.containsKey(CONF_JMX_PORT)) {
                writer.println(configs.get(CONF_JMX_PORT));
            }
        }

    }

    private boolean isComplete(Map<String, String> configs) {
        return configs.containsKey(CONF_METRICS_ENABLED) && configs.containsKey(CONF_JMX_PORT);
    }

}
