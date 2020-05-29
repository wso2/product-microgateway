package org.wso2.micro.gateway.tools.model;

import com.google.gson.annotations.SerializedName;

/**
 * model Prometheus for the Main.
 */
public class Prometheus {
    @SerializedName("jmx_port")
    private long jmxPort;

    public long getJmxPort() {
        return jmxPort;
    }

    public void setJmxPort(long jmxPort) {
        this.jmxPort = jmxPort;
    }
}
