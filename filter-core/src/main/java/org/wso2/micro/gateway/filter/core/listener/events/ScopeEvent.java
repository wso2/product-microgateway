package org.wso2.micro.gateway.filter.core.listener.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scope Event to handle updates in Scopes.
 */
public class ScopeEvent extends Event {
    private String name;
    private String displayName;
    private String description;
    private List<String> roles = new ArrayList<>();
    private Map<String, String> mappings = new HashMap<>();

    public ScopeEvent(String eventId, long timestamp, String type, int tenantId, String tenantDomain, String name,
                      String displayName, String description) {
        this.eventId = eventId;
        this.timeStamp = timestamp;
        this.type = type;
        this.tenantId = tenantId;
        this.tenantDomain = tenantDomain;
        this.name = name;
        this.displayName = displayName;
        this.description = description;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getDisplayName() {

        return displayName;
    }

    public void setDisplayName(String displayName) {

        this.displayName = displayName;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public List<String> getRoles() {

        return roles;
    }

    public void setRoles(List<String> roles) {

        this.roles = roles;
    }

    public Map<String, String> getMappings() {

        return mappings;
    }

    public void setMappings(Map<String, String> mappings) {

        this.mappings = mappings;
    }
}
