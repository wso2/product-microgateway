package org.wso2.choreo.connect.enforcer.jwks;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;


import java.util.List;

public class BackendJWKSDto {
    private boolean isEnabled = false;
    private JWKSet JWKS;

    public BackendJWKSDto() {

    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public JWKSet getJWKS() {
        if (JWKS==null) {
            return new JWKSet();
        }
        return JWKS;
    }

    public void setJWKS(List<JWK> ls) {
        this.JWKS = new JWKSet(ls);
    }
}
