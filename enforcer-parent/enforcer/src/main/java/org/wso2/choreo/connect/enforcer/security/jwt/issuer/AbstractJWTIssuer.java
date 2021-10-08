/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.enforcer.security.jwt.issuer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.common.gateway.exception.JWTGeneratorException;
import org.wso2.carbon.apimgt.common.gateway.util.JWTUtil;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.dto.JWTIssuerConfigurationDto;
import org.wso2.choreo.connect.enforcer.security.TokenValidationContext;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Abstract JWT issuer for tokens.
 */
public abstract class AbstractJWTIssuer implements TokenIssuer {
    private static final Log log = LogFactory.getLog(AbstractJWTIssuer.class);

    private static final String SHA256_WITH_RSA = "SHA256withRSA";

    private static final String NONE = "NONE";

    private static volatile long ttl = -1L;

    private String signatureAlgorithm = SHA256_WITH_RSA;

    public String dialectURI;

    JWTIssuerConfigurationDto jwtIssuerConfigurationDto;

    public AbstractJWTIssuer() {
        setJWTConfigurationDto();
    }

    public void setJWTConfigurationDto() {
        this.jwtIssuerConfigurationDto = ConfigHolder.getInstance().getConfig().getJwtIssuerConfigurationDto();
        dialectURI = jwtIssuerConfigurationDto.getConsumerDialectUri();
        signatureAlgorithm = jwtIssuerConfigurationDto.getSignatureAlgorithm();
        if (signatureAlgorithm == null || !(NONE.equals(signatureAlgorithm)
                || SHA256_WITH_RSA.equals(signatureAlgorithm))) {
            signatureAlgorithm = SHA256_WITH_RSA;
        }
    }

    public abstract Map<String, String> populateStandardClaims(TokenValidationContext validationContext)
            throws JWTGeneratorException;

    public String getDialectURI() {
        return dialectURI;
    }

    public String encode(byte[] stringToBeEncoded) throws JWTGeneratorException {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(stringToBeEncoded);
    }

    public String generateToken(TokenValidationContext validationContext) throws JWTGeneratorException {

        String jwtHeader = buildHeader();
        String base64UrlEncodedHeader = "";
        if (jwtHeader != null) {
            base64UrlEncodedHeader = encode(jwtHeader.getBytes(Charset.defaultCharset()));
        }
        String jwtBody = buildBody(validationContext);
        String base64UrlEncodedBody = "";
        if (jwtBody != null) {
            base64UrlEncodedBody = encode(jwtBody.getBytes());
        }

        if (SHA256_WITH_RSA.equals(signatureAlgorithm)) {
            String assertion = base64UrlEncodedHeader + '.' + base64UrlEncodedBody;
            // Get the assertion signed
            byte[] signedAssertion = signJWT(assertion, validationContext.getValidationInfoDTO().getEndUserName());

            if (log.isDebugEnabled()) {
                log.debug("signed assertion value : " + new String(signedAssertion, Charset.defaultCharset()));
            }
            String base64UrlEncodedAssertion = encode(signedAssertion);
            return base64UrlEncodedHeader + '.' + base64UrlEncodedBody + '.' + base64UrlEncodedAssertion;
        } else {
            return base64UrlEncodedHeader + '.' + base64UrlEncodedBody + '.';
        }
    }

    public String buildHeader() throws JWTGeneratorException {
        String jwtHeader = null;

        // If signature algo==NONE, header without cert
        if (NONE.equals(signatureAlgorithm)) {
            StringBuilder jwtHeaderBuilder = new StringBuilder();
            jwtHeaderBuilder.append("{\"typ\":\"JWT\",");
            jwtHeaderBuilder.append("\"alg\":\"");
            jwtHeaderBuilder.append(JWTUtil.getJWSCompliantAlgorithmCode(NONE));
            jwtHeaderBuilder.append('\"');
            jwtHeaderBuilder.append('}');
            jwtHeader = jwtHeaderBuilder.toString();
        } else if (SHA256_WITH_RSA.equals(signatureAlgorithm)) {
            jwtHeader = addCertToHeader();
        }
        return jwtHeader;
    }

    public String buildBody(TokenValidationContext validationContext) throws JWTGeneratorException {

        Map<String, String> standardClaims = populateStandardClaims(validationContext);
        JWTClaimsSet.Builder jwtClaimsSetBuilder = new JWTClaimsSet.Builder();

        if (standardClaims != null) {
            Iterator<String> it = new TreeSet(standardClaims.keySet()).iterator();
            while (it.hasNext()) {
                String claimURI = it.next();
                String claimVal = standardClaims.get(claimURI);
                if (claimVal != null && claimVal.contains("{")) {
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        Map<String, String> map = mapper.readValue(claimVal, Map.class);
                        jwtClaimsSetBuilder.claim(claimURI, map);
                    } catch (IOException e) {
                        // Exception isn't thrown in order to generate jwt without claim, even if an error is
                        // occurred during the retrieving claims.
                        log.error(String.format("Error while reading claim values for %s", claimVal), e);
                    }
                } else if ("exp".equals(claimURI)) {
                    jwtClaimsSetBuilder.expirationTime(new Date(Long.valueOf(standardClaims.get(claimURI))));
                } else if ("iat".equals(claimURI)) {
                    jwtClaimsSetBuilder.issueTime(new Date(Long.valueOf(standardClaims.get(claimURI))));
                } else {
                    jwtClaimsSetBuilder.claim(claimURI, claimVal);
                }
            }
            // Adding JTI standard claim
            jwtClaimsSetBuilder.jwtID(UUID.randomUUID().toString());
        }
        return jwtClaimsSetBuilder.build().toJSONObject().toJSONString();
    }

    public byte[] signJWT(String assertion, String endUserName) throws JWTGeneratorException {

        try {
            PrivateKey privateKey = jwtIssuerConfigurationDto.getPrivateKey();
            return JWTUtil.signJwt(assertion, privateKey, signatureAlgorithm);
        } catch (Exception e) {
            throw new JWTGeneratorException(e);
        }
    }

    public long getTTL() {
        if (jwtIssuerConfigurationDto.getTtl() != 0) {
            ttl = jwtIssuerConfigurationDto.getTtl();
        } else {
            // 60 * 60 (convert 60 minutes to seconds)
            ttl = Long.valueOf(3600);
        }
        return ttl;
    }

    /**
     * Helper method to add public certificate to JWT_HEADER to signature verification.
     *
     * @throws JWTGeneratorException
     */
    protected String addCertToHeader() throws JWTGeneratorException {

        try {
            Certificate publicCert = jwtIssuerConfigurationDto.getPublicCert();
            return JWTUtil.generateHeader(publicCert, signatureAlgorithm);
        } catch (Exception e) {
            String error = "Error in obtaining keystore";
            throw new JWTGeneratorException(error, e);
        }
    }
}
