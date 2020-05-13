package org.wso2.micro.gateway.tools.model;

/**
 * model Metrics for the Main.
 */
public class Metrics {
    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Prometheus getPrometheus() {
        return prometheus;
    }

    public void setPrometheus(Prometheus prometheus) {
        this.prometheus = prometheus;
    }

    private Prometheus prometheus;
}
