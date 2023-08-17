package org.wso2.choreo.connect.enforcer.security.jwt;

import com.nimbusds.jose.util.DefaultResourceRetriever;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HttpsURLConnection;

/**
 * ExtendedJWKSResourceRetriever is used to retrieve the JWKSet from
 * the JWKS endpoint with customSSLContext configured.
 */
public class ExtendedJWKSResourceRetriever extends DefaultResourceRetriever {

    private static final Log log = LogFactory.getLog(ExtendedJWKSResourceRetriever.class);

    public ExtendedJWKSResourceRetriever(int connectTimeout, int readTimeout, int sizeLimit) {
        super(connectTimeout, readTimeout, sizeLimit);
    }

    @Override
    protected HttpURLConnection openConnection(final URL url) throws IOException {
        if (url.getProtocol().equalsIgnoreCase("https")) {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();
            httpsURLConnection.setHostnameVerifier(FilterUtils.getHostnameVerifier());
            try {
                httpsURLConnection.setSSLSocketFactory(FilterUtils.createSSLContext().getSocketFactory());
            } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
                log.error("Error while creating SSL Context for the JWKS endpoint connection. " +
                        "Hence default SSLSocketFactory is used.");
                httpsURLConnection.setSSLSocketFactory(HttpsURLConnection.getDefaultSSLSocketFactory());
            }
            return httpsURLConnection;
        } else {
            return super.openConnection(url);
        }
    }
}
