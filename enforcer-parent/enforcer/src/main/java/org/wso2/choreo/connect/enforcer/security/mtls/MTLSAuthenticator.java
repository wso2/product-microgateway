/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.enforcer.security.mtls;

import io.opentelemetry.context.Scope;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.wso2.choreo.connect.enforcer.commons.exception.APISecurityException;
import org.wso2.choreo.connect.enforcer.commons.model.AuthenticationContext;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.APISecurityConstants;
import org.wso2.choreo.connect.enforcer.security.Authenticator;
import org.wso2.choreo.connect.enforcer.tracing.TracingConstants;
import org.wso2.choreo.connect.enforcer.tracing.TracingSpan;
import org.wso2.choreo.connect.enforcer.tracing.TracingTracer;
import org.wso2.choreo.connect.enforcer.tracing.Utils;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;

import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

/**
 * Implements the authenticator interface to authenticate request using MTLS.
 */
public class MTLSAuthenticator implements Authenticator {
    private static final Logger log = LogManager.getLogger(MTLSAuthenticator.class);
    private final boolean isEnableClientValidation = FilterUtils.getClientValidationStatus();
    private final boolean isClientCertificateEncode = FilterUtils.getCertificateEncodeStatus();

    @Override
    public boolean canAuthenticate(RequestContext requestContext) {
        String cert = "";

        if (isEnableClientValidation) {
            cert = requestContext.getClientCertificate();
        } else {
            if (requestContext.getHeaders().containsKey(FilterUtils.getCertificateHeaderName())) {
                cert = retrieveMTLSHeaderValue(requestContext);
            }
        }
        if (cert.isEmpty()) {
            log.debug("Could +not find a valid client certificate in the request: " + requestContext.getRequestPath());
            return false;
        }
        return true;
    }

    @Override
    public AuthenticationContext authenticate(RequestContext requestContext) throws APISecurityException {
        TracingTracer tracer;
        TracingSpan mtlsApiAuthenticatorSpan = null;
        Scope mtlsApiAuthenticatorSpanScope = null;

        try {
            if (Utils.tracingEnabled()) {
                tracer = Utils.getGlobalTracer();
                mtlsApiAuthenticatorSpan = Utils.startSpan(TracingConstants.MTLS_API_AUTHENTICATOR_SPAN, tracer);
                mtlsApiAuthenticatorSpanScope = mtlsApiAuthenticatorSpan.getSpan().makeCurrent();
                Utils.setTag(mtlsApiAuthenticatorSpan, APIConstants.LOG_TRACE_ID,
                        ThreadContext.get(APIConstants.LOG_TRACE_ID));
            }

            AuthenticationContext authenticationContext = new AuthenticationContext();
            KeyStore trustStore = retrieveTrustStore(requestContext);
            Map<String, String> mtlsCertificateTiers = retrieveMtlsCertificateTiers(requestContext);
            X509Certificate clientCert;
            boolean authenticated = false;
            String clientCertificateTier;
            String clientCertificateAlias;

            try {
                clientCert = getClientCertificate(requestContext);
                clientCertificateAlias = MtlsUtils.getMatchedCertificateAliasFromTrustStore(clientCert, trustStore);
                if (clientCertificateAlias == null) {
                    log.debug(String.format("Provided client certificate in request: %s is not in the truststore of " +
                            "the API: %s", requestContext.getRequestPath(), requestContext.getMatchedAPI().getName()));
                    clientCert = null;
                }
                if (clientCert != null) {
                    authenticated = true;
                    clientCertificateTier = MtlsUtils.getTier(clientCertificateAlias, mtlsCertificateTiers);
                    if (clientCertificateTier != null) {
                        if (!clientCertificateTier.equals("")) {
                            authenticationContext.setTier(clientCertificateTier);
                        }
                    }
                    String subjectDN = clientCert.getSubjectDN().getName();
                    authenticationContext.setUsername(subjectDN);
                }
            } catch (CertificateException e) {
                throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                        APISecurityConstants.API_AUTH_GENERAL_ERROR, "Invalid client certificate");
            }

            String apiName = requestContext.getMatchedAPI().getName();
            String apiVersion = requestContext.getMatchedAPI().getVersion();
            String apiUUID = requestContext.getMatchedAPI().getUuid();

            authenticationContext.setAuthenticated(authenticated);
            authenticationContext.setApiName(apiName);
            authenticationContext.setApiUUID(apiUUID);
            authenticationContext.setApiVersion(apiVersion);

            return authenticationContext;
        } finally {
            if (Utils.tracingEnabled()) {
                mtlsApiAuthenticatorSpanScope.close();
                Utils.finishSpan(mtlsApiAuthenticatorSpan);
            }
        }
    }

    private X509Certificate getClientCertificate(RequestContext requestContext) throws CertificateException {
        String certContent = "";

        if (isEnableClientValidation) {
            String encodedCert = requestContext.getClientCertificate();
            if (!encodedCert.isEmpty()) {
                certContent = MtlsUtils.getCertContent(encodedCert, true);
            }
        } else {
            Map<String, String> headers = requestContext.getHeaders();
            if (headers.containsKey(FilterUtils.getCertificateHeaderName())) {
                String cert = retrieveMTLSHeaderValue(requestContext);
                requestContext.setClientCertificate(cert);
                if (!cert.isEmpty()) {
                    certContent = MtlsUtils.getCertContent(cert, isClientCertificateEncode);
                }
            }
        }
        if (!certContent.isEmpty()) {
            return MtlsUtils.getX509Cert(certContent);
        }
        log.debug(String.format("Provided client certificate in the request: %s is invalid",
                requestContext.getRequestPath()));
        return null;
    }

    private String retrieveMTLSHeaderValue(RequestContext requestContext) {
        Map<String, String> headers = requestContext.getHeaders();
        return headers.get(FilterUtils.getCertificateHeaderName());
    }

    private KeyStore retrieveTrustStore(RequestContext requestContext) {
        KeyStore trustStore;
        trustStore = requestContext.getMatchedAPI().getTrustStore();
        return trustStore;
    }

    private Map<String, String> retrieveMtlsCertificateTiers(RequestContext requestContext) {
        Map<String, String> mtlsCertificateTiers;
        mtlsCertificateTiers = requestContext.getMatchedAPI().getMtlsCertificateTiers();
        return mtlsCertificateTiers;
    }

    @Override
    public String getChallengeString() {
        return "Mutual SSL realm=\"Choreo Connect\"";
    }

    @Override
    public String getName() {
        return APIConstants.API_SECURITY_MUTUAL_SSL_NAME;
    }

    @Override
    public int getPriority() {
        return -15;
    }
}
