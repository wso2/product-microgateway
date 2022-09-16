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
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.wso2.choreo.connect.enforcer.commons.exception.APISecurityException;
import org.wso2.choreo.connect.enforcer.commons.model.AuthenticationContext;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
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
import java.util.Objects;

/**
 * Implements the authenticator interface to authenticate request using MTLS.
 */
public class MTLSAuthenticator implements Authenticator {
    private static final Logger log = LogManager.getLogger(MTLSAuthenticator.class);
    private final boolean isEnableClientValidation = ConfigHolder.getInstance().getConfig().getMtlsInfo()
                                                        .isEnableClientValidation();
    private final boolean isClientCertificateEncode = ConfigHolder.getInstance().getConfig().getMtlsInfo()
                                                        .isClientCertificateEncode();

    @Override
    public boolean canAuthenticate(RequestContext requestContext) {
        String cert = "";

        if (isEnableClientValidation) {
            cert = requestContext.getClientCertificate();
        } else if (requestContext.getHeaders().containsKey(FilterUtils.getCertificateHeaderName())) {
            cert = requestContext.getHeaders().get(FilterUtils.getCertificateHeaderName());
        }
        if (StringUtils.isBlank(cert)) {
            log.debug("Could not find a valid client certificate in the request: {} for the API: {}:{} ",
                    requestContext.getMatchedResourcePaths().get(0).getPath(), requestContext.getMatchedAPI().getName(),
                    requestContext.getMatchedAPI().getVersion());
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
            KeyStore trustStore = requestContext.getMatchedAPI().getTrustStore();
            boolean authenticated = false;

            try {
                X509Certificate clientCert = getClientCertificate(requestContext);
                String clientCertificateAlias = MtlsUtils
                        .getMatchedCertificateAliasFromTrustStore(clientCert, trustStore);
                if (StringUtils.isBlank(clientCertificateAlias)) {
                    log.debug("Provided client certificate in request: {} is not in the truststore of the API: {}:{} ",
                            requestContext.getMatchedResourcePaths().get(0).getPath(),
                            requestContext.getMatchedAPI().getName(), requestContext.getMatchedAPI().getVersion());
                    clientCert = null;
                }
                if (!Objects.isNull(clientCert)) {
                    authenticated = true;
                    String clientCertificateTier = "";
                    if (requestContext.getMatchedAPI().getMtlsCertificateTiers().containsKey(clientCertificateAlias)) {
                        clientCertificateTier = requestContext.getMatchedAPI().getMtlsCertificateTiers()
                                .get(clientCertificateAlias);
                    }
                    if (StringUtils.isNotBlank(clientCertificateTier)) {
                        authenticationContext.setTier(clientCertificateTier);
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
            if (StringUtils.isNotBlank(encodedCert)) {
                certContent = MtlsUtils.getCertContent(encodedCert, true);
            }
        } else {
            Map<String, String> headers = requestContext.getHeaders();
            if (headers.containsKey(FilterUtils.getCertificateHeaderName())) {
                String cert = requestContext.getHeaders().get(FilterUtils.getCertificateHeaderName());
                requestContext.setClientCertificate(cert);
                if (StringUtils.isNotBlank(cert)) {
                    certContent = MtlsUtils.getCertContent(cert, isClientCertificateEncode);
                }
            }
        }
        if (StringUtils.isNotBlank(certContent)) {
            return MtlsUtils.getX509Cert(certContent);
        }
        log.debug("Provided client certificate in the request: {} for the API: {}:{} is invalid.",
                requestContext.getMatchedResourcePaths().get(0).getPath(), requestContext.getMatchedAPI().getName(),
                requestContext.getMatchedAPI().getVersion());
        return null;
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
