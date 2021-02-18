/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.micro.gateway.enforcer.security.jwt.validator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  Singleton which stores the revoked JWT map.
 */
public class RevokedJWTDataHolder {

    private static final Logger log = LogManager.getLogger(RevokedJWTDataHolder.class);
    private static Map<String, Long> revokedJWTMap = new ConcurrentHashMap<>();
    private static RevokedJWTDataHolder instance = new RevokedJWTDataHolder();

    /**
     * Adds a given key,value pair to the revoke map.
     * @param key key to be added.
     * @param value value to be added.
     */
    public void addRevokedJWTToMap(String key, Long value) {
        if (key != null && value != null) {
            log.debug("Adding revoked JWT key, value pair to the revoked map :" + key + " , " + value);
            revokedJWTMap.put(key, value);
        }
    }

    /**
     * Checks whether a given signature is in the map.
     * @param jwtSignature signature to be checked.
     * @return true if it exists and false otherwise.
     */
    public static boolean isJWTTokenSignatureExistsInRevokedMap(String jwtSignature) {
        return revokedJWTMap.containsKey(jwtSignature);
    }

    private RevokedJWTDataHolder() {

    }

    /**
     * Fetches the revoke map.
     * @return
     */
    Map<String, Long> getRevokedJWTMap() {
        return revokedJWTMap;
    }

    /**
     * This method can be used to get the singleton instance of this class.
     * @return the singleton instance.
     */
    public static RevokedJWTDataHolder getInstance() {
        return instance;
    }
}
