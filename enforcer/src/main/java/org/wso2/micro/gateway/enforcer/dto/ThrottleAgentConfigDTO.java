package org.wso2.micro.gateway.enforcer.dto;

import org.wso2.micro.gateway.enforcer.globalthrottle.databridge.agent.conf.AgentConfiguration;
import org.wso2.micro.gateway.enforcer.globalthrottle.databridge.publisher.PublisherConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * This contains throttle configurations.
 */
public class ThrottleAgentConfigDTO {
    boolean enabled = false;
    String username;
    String password;
    List<ThrottleURLGroupDTO> urlGroup = new ArrayList<>();
    PublisherConfiguration publisher;
    AgentConfiguration agent;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<ThrottleURLGroupDTO> getUrlGroup() {
        return urlGroup;
    }

    public void setUrlGroup(List<ThrottleURLGroupDTO> urlGroup) {
        this.urlGroup = urlGroup;
    }

    public PublisherConfiguration getPublisher() {
        return publisher;
    }

    public void setPublisher(PublisherConfiguration publisher) {
        this.publisher = publisher;
    }

    public AgentConfiguration getAgent() {
        return agent;
    }

    public void setAgent(AgentConfiguration agent) {
        this.agent = agent;
    }
}
