package org.wso2.choreo.connect.enforcer.commons.model;

/**
 * This class represents an extended operation in the API.
 */
public class ExtendedOperation {
    private String name;
    private String verb;
    private String description;
    private String mode;
    private String schema;
    private String apiName;
    private String apiVersion;
    private String apiContext;
    private String apiTarget;
    private String apiVerb;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVerb() {
        return verb;
    }

    public void setVerb(String verb) {
        this.verb = verb;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getApiContext() {
        return apiContext;
    }

    public void setApiContext(String apiContext) {
        this.apiContext = apiContext;
    }

    public String getApiTarget() {
        return apiTarget;
    }

    public void setApiTarget(String apiTarget) {
        this.apiTarget = apiTarget;
    }

    public String getApiVerb() {
        return apiVerb;
    }

    public void setApiVerb(String apiVerb) {
        this.apiVerb = apiVerb;
    }


}
