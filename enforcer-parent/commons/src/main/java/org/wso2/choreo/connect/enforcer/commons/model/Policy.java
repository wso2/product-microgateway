package org.wso2.choreo.connect.enforcer.commons.model;

import java.util.Map;

/**
 * operational policy.
 */
public class Policy {

    private String policyName;
    private String templateName;
    private int order;
    private Map<String, String> parameters;

    public Policy() {
    }

    public Policy(String policyName, String templateName, int order, Map<String, String> parameters) {
        this.policyName = policyName;
        this.templateName = templateName;
        this.order = order;
        this.parameters = parameters;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
}
