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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.constants.HashingConstants;
import org.wso2.apimgt.gateway.cli.exception.HashingException;
import org.wso2.apimgt.gateway.cli.model.rest.ext.ExtendedAPI;
import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;
import org.wso2.carbon.apimgt.rest.api.admin.dto.ApplicationThrottlePolicyDTO;
import org.wso2.carbon.apimgt.rest.api.admin.dto.SubscriptionThrottlePolicyDTO;
import org.wso2.carbon.apimgt.rest.api.admin.dto.ThrottlePolicyDTO;
import org.wso2.carbon.apimgt.rest.api.publisher.dto.APIInfoDTO;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Utility class used for hashing functionality
 * 
 */
public class HashUtils {

    private static final Logger logger = LoggerFactory.getLogger(HashUtils.class);

    /**
     * Generate hashes for the specified apis, subscription and application policies, then compare with the previously
     *  generated hashes and detect if there are changes with them.
     * 
     * @param apis APIs list
     * @param subscriptionPolicies Subscription Policies list
     * @param appPolicies Application policies list
     * @param projectName Name of the project
     * @return true if there are changes detected vs the previous check
     * @throws HashingException error while change detection
     */
    public static boolean detectChanges(List<APIInfoDTO> apis,
            List<SubscriptionThrottlePolicyDTO> subscriptionPolicies,
            List<ApplicationThrottlePolicyDTO> appPolicies, String projectName) throws HashingException {
        
        boolean hasChanges = true;
        Map<String, String> allHashesMap = new HashMap<>();
        Map<String, String> apiHashesMap = getMapOfHashes(apis);
        Map<String, String> appPolicyHashesMap = getMapOfHashes(appPolicies);
        Map<String, String> subsPolicyHashesMap = getMapOfHashes(subscriptionPolicies);

        logger.debug("API calculated hashes {}", apiHashesMap);
        logger.debug("App policy calculated hashes {}", appPolicyHashesMap);
        logger.debug("Subscription policy calculated hashes {}", subsPolicyHashesMap);

        allHashesMap.putAll(apiHashesMap);
        allHashesMap.putAll(appPolicyHashesMap);
        allHashesMap.putAll(subsPolicyHashesMap);

        try {
            Map<String, String> storedHashes = loadStoredResourceHashes(projectName);
            if (equalMaps(storedHashes, allHashesMap)) {
                logger.debug("No changes detected after calculating hashes.");
                hasChanges = false;
            } else {
                logger.debug("Storing calculated resource hashes.");
                storeResourceHashes(allHashesMap, projectName);
                logger.debug("Storing calculated resource hashes success.");
            }
        } catch (IOException e) {
            throw new HashingException("Error while resource change detection", e);
        }
        return hasChanges;
    }

    /**
     * Loads the stored resource hashes
     * 
     * @param projectName name of the project
     * 
     * @return a map with id to hash mapping loaded from the CLI temp
     * @throws IOException error while loading the stored hashes
     */
    private static Map<String, String> loadStoredResourceHashes(String projectName) throws IOException {
        String content = GatewayCmdUtils.loadStoredResourceHashes(projectName);
        Map<String, String> hashes = new HashMap<>();
        if (StringUtils.isNotEmpty(content)) {
            ObjectMapper objectMapper = new ObjectMapper();
            hashes = objectMapper.readValue(content, Map.class);
        }
        return hashes;
    }

    /**
     * Store the calculated hashes of API/policy resources in CLI temp folder
     * 
     * @param hashesMap map of id against hashes to be stored
     * @param projectName name of the project
     * @throws IOException error while storing hash values
     */
    private static void storeResourceHashes(Map<String, String> hashesMap, String projectName) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String stringifiedHashes = mapper.writeValueAsString(hashesMap);
        GatewayCmdUtils.storeResourceHashesFileContent(stringifiedHashes, projectName);
    }

    /**
     * Calculate the hashes of the given list of APIs or Throttle policies and return as a map with id -> hash mapping 
     * 
     * @param objects List of APIs
     * @return map with id -> hash mapping
     * @throws HashingException error while calculating hashes of the APIs
     */
    private static Map<String, String> getMapOfHashes(List objects) throws HashingException {
        Map<String, String> hashes = new HashMap<>();
        if (objects != null) {
            for (Object obj : objects) {
                String hash = getAnnotatedHash(obj);
                if (obj instanceof ExtendedAPI) {
                    hashes.put(((ExtendedAPI)obj).getId(), hash);
                } else if (obj instanceof ThrottlePolicyDTO) {
                    hashes.put(((ThrottlePolicyDTO)obj).getId(), hash);
                } else {
                    logger.warn("Incompatible type for generating hash: " + obj + ", class: " + obj.getClass());
                }
            }
        }
        return hashes;
    }

    /**
     * Calculates the hash of the object using the Hash annotations added to the getter methods of the object.
     * 
     * @param obj object whose hash needs to be calculated. 
     * @return calculated hash value
     * @throws HashingException error while calculating hash
     */
    private static String getAnnotatedHash(Object obj) throws HashingException {
        ObjectMapper mapper = new ObjectMapper();
        TreeSet<String> sortedSet = new TreeSet<>();
        for (Method method : obj.getClass().getMethods()) {
            if (method.isAnnotationPresent(Hash.class)) {
                try {
                    Object value = method.invoke(obj);
                    String stringifiedField = mapper.writeValueAsString(value);
                    
                    //The method name needs to be added here. The reason is, the array of methods returned from 
                    // getClass().getMethods() is not always in a particular order. If the order changes, this would 
                    // result in change of the hash even through the object didn't change. To fix this, a TreeSet is 
                    // used which will insert entries sorted in natural ordering. The method name appended in the front
                    // to always have a fixed string at the beginning.
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
