/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.apimgt.gateway.cli.hashing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.wso2.apimgt.gateway.cli.constants.GatewayCliConstants;
import org.wso2.apimgt.gateway.cli.constants.HashingConstants;
import org.wso2.apimgt.gateway.cli.exception.HashingException;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;
import org.wso2.apimgt.gateway.cli.model.rest.policy.ApplicationThrottlePolicyDTO;
import org.wso2.apimgt.gateway.cli.model.rest.policy.SubscriptionThrottlePolicyDTO;
import org.wso2.apimgt.gateway.cli.model.rest.policy.ThrottlePolicyDTO;
import org.wso2.apimgt.gateway.cli.model.template.policy.ThrottlePolicy;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class HashUtils {

    public static boolean detectChanges(List<ExtendedAPI> apis,
            List<SubscriptionThrottlePolicyDTO> subscriptionPolicies,
            List<ApplicationThrottlePolicyDTO> appPolicies) throws HashingException {
        boolean hasChanges = true;
        Map<String, String> allHashesMap = new HashMap<>();
        Map<String, String> apiHashesMap = getMapOfHashesForAPIs(apis);
        Map<String, String> appPolicyHashesMap = getMapOfHashesForAppPolicies(appPolicies);
        Map<String, String> subsPolicyHashesMap = getMapOfHashesForSubpolicies(subscriptionPolicies);

        allHashesMap.putAll(apiHashesMap);
        allHashesMap.putAll(appPolicyHashesMap);
        allHashesMap.putAll(subsPolicyHashesMap);

        try {
            Map<String, String> storedHashes = loadStoredResourceHashes();
            if (equalMaps(storedHashes, allHashesMap)) {
                hasChanges = false;
            } else {
                storeResourceHashes(allHashesMap);
            }
        } catch (IOException e) {
            throw new HashingException("Error while resource change detection", e);
        }
        return hasChanges;
    }

    private static Map<String, String> loadStoredResourceHashes() throws IOException {
        String content = GatewayCmdUtils.loadStoredResourceHashes();
        Map<String, String> hashes = new HashMap<>();
        if (StringUtils.isNotEmpty(content)) {
            ObjectMapper objectMapper = new ObjectMapper();
            hashes = objectMapper.readValue(content, Map.class);
        }
        return hashes;
    }

    private static void storeResourceHashes(Map<String, String> hashesMap) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String stringifiedHashes = mapper.writeValueAsString(hashesMap);
        GatewayCmdUtils.storeResourceHashesFileContent(stringifiedHashes);
    }

    private static Map<String, String> getMapOfHashesForAPIs(List<ExtendedAPI> apis) throws HashingException {
        Map<String, String> hashes = new HashMap<>();
        if (apis != null) {
            for (ExtendedAPI api : apis) {
                String hash = getAnnotatedHash(api);
                System.out.println(api.getName() + " -- " + hash);
                hashes.put(api.getId(), hash);
            }
        }
        return hashes;
    }

    private static Map<String, String> getMapOfHashesForAppPolicies(List<ApplicationThrottlePolicyDTO> policies)
            throws HashingException {
        Map<String, String> hashes = new HashMap<>();
        if (policies != null) {
            for (ApplicationThrottlePolicyDTO throttlePolicy : policies) {
                String hash = getAnnotatedHash(throttlePolicy);
                System.out.println(throttlePolicy.getDisplayName() + " -- " + hash);
                hashes.put(throttlePolicy.getPolicyId(), hash);
            }
        }
        return hashes;
    }

    private static Map<String, String> getMapOfHashesForSubpolicies(List<SubscriptionThrottlePolicyDTO> policies)
            throws HashingException {
        Map<String, String> hashes = new HashMap<>();
        if (policies != null) {
            for (SubscriptionThrottlePolicyDTO throttlePolicy : policies) {
                String hash = getAnnotatedHash(throttlePolicy);
                System.out.println(throttlePolicy.getDisplayName() + " -- " + hash);
                hashes.put(throttlePolicy.getPolicyId(), hash);
            }
        }
        return hashes;
    }
    
    private static String getAnnotatedHash(Object obj) throws HashingException {
        ObjectMapper mapper = new ObjectMapper();
        TreeSet<String> sortedSet = new TreeSet<>();
        for (Method method : obj.getClass().getMethods()) {
            if (method.isAnnotationPresent(Hash.class)) {
                try {
                    Object value = method.invoke(obj);
                    String stringifiedField = mapper.writeValueAsString(value);
                    sortedSet.add(method.getName() + HashingConstants.HASH_SEPARATOR + stringifiedField);
                } catch (JsonProcessingException | IllegalAccessException | InvocationTargetException e) {
                    throw new HashingException("Error while generating hash for " + obj, e);
                }
            }
        }
        StringBuilder builder = new StringBuilder();
        for (String entry : sortedSet) {
            builder.append(entry);
            builder.append(HashingConstants.HASH_SEPARATOR);
        }
        String val = builder.toString();
        return getMD5Hex(val);
    }

    /**
     * Iterate through the given maps and check if they are both having equal entries 
     * 
     * @param map1 First map
     * @param map2 Second map
     * @return true if both maps are having equal entries 
     */
    private static boolean equalMaps(Map<String, String> map1, Map<String, String> map2) {
        if (map1 == null || map2 == null) {
            return false;
        }
        if (map1.keySet().size() != map2.keySet().size()) {
            return false;
        }

        for (String map1Key : map1.keySet()) {
            if (map1.get(map1Key) == null || !map1.get(map1Key).equals(map2.get(map1Key))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates the MD5 hash for a given String
     * 
     * @param inputString input string
     * @return calculated md5 hash value
     * @throws HashingException error while calculating the md5 hash value
     */
    private static String getMD5Hex(final String inputString) throws HashingException {
        MessageDigest md;
        byte[] digest;
        try {
            md = MessageDigest.getInstance(HashingConstants.HASH_ALGORITHM);
            md.update(inputString.getBytes());
            digest = md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new HashingException("Error getting md5 hash of " + inputString, e);
        }
        return convertByteToHex(digest);
    }


    /**
     * Convert the given byte array to a hex string
     * 
     * @param byteData byte array
     * @return converted hex string for the byte array
     */
    private static String convertByteToHex(byte[] byteData) {
        StringBuilder sb = new StringBuilder();
        for (byte aByteData : byteData) {
            sb.append(Integer.toString((aByteData & 0xff) + 0x100, HashingConstants.BASE_16).substring(1));
        }

        return sb.toString();
    }
}
