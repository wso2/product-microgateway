package org.wso2.choreo.connect.enforcer.jwks;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;

import java.util.List;

/**
 * Configuration holder for Backend JWKS endpoint
 */
public class BackendJWKSDto {
    private boolean isEnabled = false;
    private JWKSet jwks;

    public BackendJWKSDto() {

    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public JWKSet getJwks() {
        if (jwks == null) {
            this.jwks = new JWKSet();
        }
        return jwks;
    }

    public void setJwks(List<JWK> ls) {
        this.jwks = new JWKSet(ls);
    }
}
