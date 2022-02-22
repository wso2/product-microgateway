package org.wso2.choreo.connect.enforcer.interceptor.opa;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.commons.logging.ErrorDetails;
import org.wso2.choreo.connect.enforcer.commons.logging.LoggingConstants;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.commons.opa.OPARequestGenerator;
import org.wso2.choreo.connect.enforcer.commons.opa.OPASecurityException;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.APISecurityConstants;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * HTTP Client which send requests to OPA server by selecting the implementation of {@link OPARequestGenerator}
 * which is provided with policy attributes.
 */
public class OPAClient {
    private static final Logger log = LogManager.getLogger(OPAClient.class);
    private static final String DEFAULT_REQUEST_GENERATOR_CLASS =
            "org.wso2.choreo.connect.enforcer.commons.model.RequestContext.OPADefaultRequestGenerator";
    private static final OPAClient opaClient = new OPAClient();

    private final OPARequestGenerator defaultRequestGenerator = new OPADefaultRequestGenerator();
    private final Map<String, OPARequestGenerator> requestGeneratorMap = new HashMap<>();

    private OPAClient() {
    }

    public static void init() {
        getInstance().loadRequestGenerators();
    }

    public static OPAClient getInstance() {
        return opaClient;
    }

    public boolean validateRequest(RequestContext requestContext, Map<String, String> policyAttrib)
            throws OPASecurityException {
        String requestGeneratorClassName = policyAttrib.get("requestGenerator");
        OPARequestGenerator requestGenerator = requestGeneratorMap.get(requestGeneratorClassName);
        if (requestGenerator == null) {
            log.error("OPA Request Generator Implementation is not found in the classPath under the provided name: {}",
                    requestGeneratorClassName, ErrorDetails.errorLog(LoggingConstants.Severity.MINOR, 6103));
            throw new OPASecurityException(APIConstants.StatusCodes.INTERNAL_SERVER_ERROR.getCode(),
                    APISecurityConstants.REMOTE_AUTHORIZATION_REQUEST_FAILURE,
                    "Error creating request to remote authorization service");
        }

        String serverUrl = policyAttrib.get("serverUrl");
        String token = policyAttrib.get("accessToken");
        String policyName = policyAttrib.get("policy");
        String ruleName = policyAttrib.get("rule");
        // TODO: (renuka) handle additionalProperties, check with APIM

        String requestBody = requestGenerator.generateRequest(policyName, ruleName, null, requestContext);
        String evaluatingPolicyUrl = serverUrl + "/" + policyName + "/" + ruleName; // including multiple "/" is fine.
        String opaResponse = callOPAServer(evaluatingPolicyUrl, requestBody, token);
        return requestGenerator.handleResponse(policyName, ruleName, opaResponse, requestContext);
    }

    private void loadRequestGenerators() {
        ServiceLoader<OPARequestGenerator> loader = ServiceLoader.load(OPARequestGenerator.class);
        for (OPARequestGenerator generator : loader) {
            requestGeneratorMap.put(generator.getClass().getName(), generator);
        }
        requestGeneratorMap.put("", defaultRequestGenerator);
        requestGeneratorMap.put(null, defaultRequestGenerator);
        requestGeneratorMap.put(DEFAULT_REQUEST_GENERATOR_CLASS, defaultRequestGenerator);
    }

    private static String callOPAServer(String serverEp, String payload, String token) throws OPASecurityException {
        try {
            URL url = new URL(serverEp);
            KeyStore opaKeyStore = ConfigHolder.getInstance().getOpaKeyStore();
            try (CloseableHttpClient httpClient = (CloseableHttpClient) FilterUtils.getHttpClient(url.getProtocol(),
                    opaKeyStore)) {
                HttpPost httpPost = new HttpPost(serverEp);
                HttpEntity reqEntity = new ByteArrayEntity(payload.getBytes(Charset.defaultCharset()));
                httpPost.setEntity(reqEntity);
                httpPost.setHeader(APIConstants.CONTENT_TYPE_HEADER, APIConstants.APPLICATION_JSON);
                if (StringUtils.isNotEmpty(token)) {
                    httpPost.setHeader(APIConstants.AUTHORIZATION_HEADER_DEFAULT, token);
                }
                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == 200) {
                        HttpEntity entity = response.getEntity();
                        try (InputStream content = entity.getContent()) {
                            return IOUtils.toString(content, Charset.defaultCharset());
                        }
                    } else {
                        log.error("Unexpected HTTP response code responded by the OPA server, HTTP code: {}",
                                statusCode, ErrorDetails.errorLog(LoggingConstants.Severity.MINOR, 6106));
                        throw new OPASecurityException(APIConstants.StatusCodes.INTERNAL_SERVER_ERROR.getCode(),
                                APISecurityConstants.REMOTE_AUTHORIZATION_REQUEST_FAILURE,
                                "Error while calling remote authorization server");
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error calling the OPA server with server endpoint: {}", serverEp,
                    ErrorDetails.errorLog(LoggingConstants.Severity.MINOR, 6104));
            throw new OPASecurityException(APIConstants.StatusCodes.INTERNAL_SERVER_ERROR.getCode(),
                    APISecurityConstants.REMOTE_AUTHORIZATION_REQUEST_FAILURE,
                    "Error while calling remote authorization server", e);
        }
    }
}
