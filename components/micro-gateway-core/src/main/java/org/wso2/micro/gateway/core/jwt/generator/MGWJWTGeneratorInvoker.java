/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.micro.gateway.core.jwt.generator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ballerinalang.jvm.values.ArrayValue;
import org.ballerinalang.jvm.values.MapValue;
import org.wso2.micro.gateway.core.Constants;
import org.wso2.micro.gateway.jwt.generator.AbstractMGWJWTGenerator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to dynamically invoke the jwt generators defined.
 */
public class MGWJWTGeneratorInvoker {
    private static final Logger log = LogManager.getLogger(MGWJWTGeneratorInvoker.class);
    private static AbstractMGWJWTGenerator abstractMGWJWTGenerator;

    public static boolean loadJWTGeneratorClass(String className,
                                                String dialectURI,
                                                String signatureAlgorithm,
                                                String keyStorePath,
                                                String keyStorePassword,
                                                String certificateAlias,
                                                String privateKeyAlias,
                                                int jwtExpiryTime,
                                                ArrayValue restrictedClaims,
                                                boolean cacheEnabled,
                                                int cacheExpiry,
                                                String tokenIssuer,
                                                ArrayValue tokenAudience) {
        try {
            Class jwtGeneratorClass = MGWJWTGeneratorInvoker.class.getClassLoader().loadClass(className);
            Constructor classConstructor = jwtGeneratorClass
                    .getDeclaredConstructor(String.class, String.class, String.class, String.class, String.class,
                            String.class, int.class, String[].class, boolean.class, int.class, String.class,
                            String[].class);
            Object[] restrictedClaimObjectArray = convertArrayValueToArray(restrictedClaims);
            String[] restrictedClaimArray = Arrays.copyOf(restrictedClaimObjectArray,
                                                            restrictedClaimObjectArray.length,
                                                            String[].class);
            Object[] tokenAudienceObjectArray = convertArrayValueToArray(tokenAudience);
            String[] tokenAudienceArray = Arrays.copyOf(tokenAudienceObjectArray,
                                                        tokenAudienceObjectArray.length,
                                                        String[].class);
            abstractMGWJWTGenerator = (AbstractMGWJWTGenerator) classConstructor
                    .newInstance(dialectURI, signatureAlgorithm, keyStorePath, keyStorePassword, certificateAlias,
                            privateKeyAlias, jwtExpiryTime, restrictedClaimArray, cacheEnabled, cacheExpiry,
                            tokenIssuer, tokenAudienceArray);
            return true;
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException
                    | InvocationTargetException | NoSuchMethodException e) {
            log.error("Error while loading the jwt generator class: " + className, e);
        }
        return false;
    }

    /**
     * Invoke token generation method.
     */
    public static String invokeGenerateToken(MapValue jwtInfo, MapValue apiDetails) throws Exception {
        Map<String, Object> jwtInfoMap = convertMapValueToMap(jwtInfo);
        Map<String, Object> apiDetailsMap = convertMapValueToMap(apiDetails);
        abstractMGWJWTGenerator.setApiDetails(apiDetailsMap);
        return abstractMGWJWTGenerator.generateToken(jwtInfoMap);
    }

    public static String invokeGetKeystorePath(String fullPath) {
        return getKeyStorePath(fullPath);
    }

    /**
     * Convert ArrayValue to Array.
     */
    public static Object[] convertArrayValueToArray(ArrayValue arrayValue) {
        Object[] array = new Object[arrayValue.size()];
        for (int i = 0; i < arrayValue.size(); i++) {
            if (arrayValue.get(i) instanceof MapValue) {
                array[i] = convertMapValueToMap((MapValue) arrayValue.get(i));
            } else if (arrayValue.get(i) instanceof ArrayValue) {
                array[i] = convertArrayValueToArray((ArrayValue) arrayValue.get(i));
            } else {
                array[i] = arrayValue.get(i);
            }
        }
        return array;
    }

    /**
     * Convert MapValue to Map.
     */
    public static Map<String, Object> convertMapValueToMap(MapValue mapValue) {
        Map<String, Object> map = new HashMap<>();
        for (Object key: mapValue.getKeys()) {
            Object valueObject = mapValue.get(key.toString());
            if (valueObject != null && valueObject instanceof MapValue) {
                MapValue subMapValue = mapValue.getMapValue(key.toString());
                Map<String, Object> subMap = convertMapValueToMap(subMapValue);
                map.put(key.toString(), subMap);
            } else if (valueObject != null && valueObject instanceof ArrayValue) {
                ArrayValue arrayValue = mapValue.getArrayValue(key.toString());
                map.put(key.toString(), convertArrayValueToArray(arrayValue));
            } else {
                map.put(key.toString(), valueObject);
            }
        }
        return map;
    }

    /**
     * Used to get the keystore path.
     */
    public static String getKeyStorePath(String fullPath) {
        String homePathConst = "\\$\\{mgw-runtime.home}";
        String homePath = System.getProperty(Constants.RUNTIME_HOME_PATH);
        return fullPath.replaceAll(homePathConst, homePath);
    }
}
