package org.wso2.choreo.connect.enforcer.commons.model;

/**
 * ChoreoComponentInfo class contains the information about the component.
 */
public class ChoreoComponentInfo {
    private String organizationID;
    private String projectID;
    private String componentID;
    private String versionID;

    public String getOrganizationID() {
        return organizationID;
    }

    public void setOrganizationID(String organizationID) {
        this.organizationID = organizationID;
    }

    public String getProjectID() {
        return projectID;
    }

    public void setProjectID(String projectID) {
        this.projectID = projectID;
    }

    public String getComponentID() {
        return componentID;
    }

    public void setComponentID(String componentID) {
        this.componentID = componentID;
    }

    public String getVersionID() {
        return versionID;
    }

    public void setVersionID(String versionID) {
        this.versionID = versionID;
    }
}
