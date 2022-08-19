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

package org.wso2.choreo.connect.enforcer.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTConfigurationDto;
import org.wso2.carbon.apimgt.common.gateway.dto.JWTInfoDto;
import org.wso2.carbon.apimgt.common.gateway.exception.JWTGeneratorException;
import org.wso2.carbon.apimgt.common.gateway.jwtgenerator.APIMgtGatewayJWTGeneratorImpl;
import org.wso2.carbon.apimgt.common.gateway.jwtgenerator.AbstractAPIMgtGatewayJWTGenerator;
import org.wso2.carbon.apimgt.common.gateway.jwttransformer.JWTTransformer;
import org.wso2.choreo.connect.enforcer.common.CacheProvider;
import org.wso2.choreo.connect.enforcer.commons.exception.APISecurityException;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.APISecurityConstants;
import org.wso2.choreo.connect.enforcer.security.jwt.JwtTransformerAnnotation;
import org.wso2.choreo.connect.enforcer.security.jwt.validator.JWTConstants;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Contains Util methods related to backend JWT generation.
 */
public class BackendJwtUtils {
    private static final Logger log = LogManager.getLogger(BackendJwtUtils.class);

    /**
     * Generates or gets the Cached Backend JWT token.
     *
     * @param jwtGenerator the jwtGenerator instance to use if generating the token
     * @param tokenSignature token signature to use in the cache key
     * @param jwtInfoDto information to include in the jwt
     * @param isGatewayTokenCacheEnabled whether gateway token cache is enabled
     * @return backend jwt token
     * @throws APISecurityException if an error occurs while generating the token
     */
    public static String generateAndRetrieveJWTToken(AbstractAPIMgtGatewayJWTGenerator jwtGenerator,
                                                     String tokenSignature, JWTInfoDto jwtInfoDto,
                                               boolean isGatewayTokenCacheEnabled) throws APISecurityException {
        log.debug("Inside generateAndRetrieveJWTToken");
        String endUserToken = null;
        boolean valid = false;
        String jwtTokenCacheKey = jwtInfoDto.getApiContext().concat(":").concat(jwtInfoDto.getVersion()).concat(":")
                .concat(tokenSignature); // TODO: (suksw) Check if to add tenantName or label also

        if (jwtGenerator != null) {
            if (isGatewayTokenCacheEnabled) {
                try {
                    Object token = CacheProvider.getGatewayJWTTokenCache().get(jwtTokenCacheKey);
                    if (token != null && !JWTConstants.UNAVAILABLE.equals(token)) {
                        endUserToken = (String) token;
                        valid = !JWTUtils.isExpired(endUserToken);
                    }
                } catch (Exception e) {
                    log.error("Error while getting token from the cache", e);
                }

                if (StringUtils.isEmpty(endUserToken) || !valid) {
                    endUserToken = generateToken(jwtGenerator, jwtInfoDto, true, jwtTokenCacheKey);
                }
            } else {
                endUserToken = generateToken(jwtGenerator, jwtInfoDto, false, jwtTokenCacheKey);
            }
        } else {
            log.debug("Error while loading JWTGenerator");
        }
        return endUserToken;
    }

    private static String generateToken(AbstractAPIMgtGatewayJWTGenerator jwtGenerator, JWTInfoDto jwtInfoDto,
                   boolean isGatewayTokenCacheEnabled, String jwtTokenCacheKey) throws APISecurityException {
        String endUserToken;
        JWTConfigurationDto jwtConfigurationDto = ConfigHolder.getInstance().getConfig().
                getJwtConfigurationDto();
        jwtGenerator.setJWTConfigurationDto(jwtConfigurationDto);
        try {
            endUserToken = jwtGenerator.generateToken(jwtInfoDto);
            if (isGatewayTokenCacheEnabled) {
                CacheProvider.getGatewayJWTTokenCache().put(jwtTokenCacheKey, endUserToken);
            }
        } catch (JWTGeneratorException e) {
            log.error("Error while Generating Backend JWT", e);
            throw new APISecurityException(APIConstants.StatusCodes.UNAUTHENTICATED.getCode(),
                    APISecurityConstants.API_AUTH_GENERAL_ERROR,
                    APISecurityConstants.API_AUTH_GENERAL_ERROR_MESSAGE, e);
        }
        return endUserToken;
    }

    /**
     * Load the specified backend JWT Generator.
     *
     * @return an instance of the JWT Generator given in the config
     */
    public static AbstractAPIMgtGatewayJWTGenerator getApiMgtGatewayJWTGenerator() {
        JWTConfigurationDto jwtConfigurationDto = ConfigHolder.getInstance().getConfig().getJwtConfigurationDto();
        String classNameInConfig = jwtConfigurationDto.getGatewayJWTGeneratorImpl();
        AbstractAPIMgtGatewayJWTGenerator jwtGenerator = null;

        // Load default jwt generator class
        if (classNameInConfig.equals(JWTConstants.DEFAULT_JWT_GENERATOR_CLASS_NAME)) {
            jwtGenerator = new APIMgtGatewayJWTGeneratorImpl();
            return jwtGenerator;
        } else {
            Class<AbstractAPIMgtGatewayJWTGenerator> clazz;
            try {
                clazz = (Class<AbstractAPIMgtGatewayJWTGenerator>) Class.forName(classNameInConfig);
                Constructor<AbstractAPIMgtGatewayJWTGenerator> constructor = clazz.getConstructor();
                jwtGenerator = constructor.newInstance();
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                    | InstantiationException | InvocationTargetException | ClassCastException e) {
                log.error("Error while generating AbstractAPIMgtGatewayJWTGenerator from the class", e);
            }
        }
        return jwtGenerator;
    }

    /**
     * Load the specified JWT Transformers.
     *
     * @return a map of JWT Transformers
     */
    public static Map<String, JWTTransformer> loadJWTTransformers() {
        ServiceLoader<JWTTransformer> loader = ServiceLoader.load(JWTTransformer.class);
        Iterator<JWTTransformer> classIterator = loader.iterator();
        Map<String, JWTTransformer> jwtTransformersMap = new HashMap<>();

        if (!classIterator.hasNext()) {
            log.debug("No JWTTransformers found.");
            return jwtTransformersMap;
        }

        while (classIterator.hasNext()) {
            JWTTransformer transformer = classIterator.next();
            Annotation[] annotations = transformer.getClass().getAnnotations();
            if (annotations.length == 0) {
                log.debug("JWTTransformer is discarded as no annotations found : {}",
                        transformer.getClass().getCanonicalName());
                continue;
            }
            for (Annotation annotation : annotations) {
                if (annotation instanceof JwtTransformerAnnotation) {
                    JwtTransformerAnnotation jwtTransformerAnnotation =
                            (JwtTransformerAnnotation) annotation;
                    if (jwtTransformerAnnotation.enabled()) {
                        log.debug("JWTTransformer for the issuer : {} is enabled.",
                                jwtTransformerAnnotation.issuer());
                        jwtTransformersMap.put(jwtTransformerAnnotation.issuer(), transformer);
                    } else {
                        log.debug("JWTTransformer for the issuer : {} is disabled.",
                                jwtTransformerAnnotation.issuer());
                    }
                }
            }
        }
        return jwtTransformersMap;
    }
}
