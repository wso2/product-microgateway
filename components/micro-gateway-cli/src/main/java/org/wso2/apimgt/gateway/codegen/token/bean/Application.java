package org.wso2.apimgt.gateway.codegen.token.bean;

import java.util.List;

public class Application {

    private String name;
    private String clientId;
    private char[] clientSecret;
    private String redirectUris;
    private List<String> grantTypes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public char[] getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(char[] clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(String redirectUris) {
        this.redirectUris = redirectUris;
    }

    public List<String> getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(List<String> grantTypes) {
        this.grantTypes = grantTypes;
    }

}
