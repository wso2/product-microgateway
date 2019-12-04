package org.wso2.micro.gateway.tools.model;

/**
 * model Observability for the Main
 */
public class Observability {
    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    private Metrics metrics;
}
