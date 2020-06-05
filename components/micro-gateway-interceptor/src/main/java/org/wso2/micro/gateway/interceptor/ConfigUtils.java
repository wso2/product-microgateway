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

package org.wso2.micro.gateway.interceptor;

import org.ballerinalang.jvm.values.ArrayValueImpl;
import org.ballerinalang.jvm.values.ErrorValue;
import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.stdlib.config.Contains;
import org.ballerinalang.stdlib.config.GetConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Set of utilities to read the configurations.
 */
public class ConfigUtils {

    private static final Logger log = LoggerFactory.getLogger("ballerina");

    /**
     * Retrieves the specified configuration value as a string.
     *
     * @param key          The configuration to be retrieved.
     * @param defaultValue The default value to be use in case there is no mapping for the provided key.
     * @return Configuration value mapped by the key.
     */
    public static String getAsString(String key, String defaultValue) {
        try {
            boolean contains = Contains.contains(key);
            if (contains) {
                Object configValue = GetConfig.get(key, "STRING");
                return configValue.toString();
            }
        } catch (ErrorValue e) {
            log.error("Error while reading config string key : " + key, e);
        }
        String value = lookUpEnvVariable(key);
        return value != null ? value : defaultValue;

    }

    /**
     * Retrieves the specified configuration value as an int.
     *
     * @param key          The configuration to be retrieved.
     * @param defaultValue The default value to be use in case there is no mapping for the provided key.
     * @return Configuration value mapped by the key.
     */
    public static int getAsInt(String key, int defaultValue) {
        try {
            boolean contains = Contains.contains(key);
            if (contains) {
                Object configValue = GetConfig.get(key, "INT");
                return Integer.parseInt(configValue.toString());
            }
        } catch (ErrorValue e) {
            log.error("Error while reading config integer key : " + key, e);
        }
        String value = lookUpEnvVariable(key);
        return value != null ? Integer.parseInt(value) : defaultValue;

    }

    /**
     * Retrieves the specified configuration value as a boolean.
     *
     * @param key          The configuration to be retrieved.
     * @param defaultValue The default value to be use in case there is no mapping for the provided key.
     * @return Configuration value mapped by the key.
     */
    public static boolean getAsBoolean(String key, boolean defaultValue) {
        try {
            boolean contains = Contains.contains(key);
            if (contains) {
                Object configValue = GetConfig.get(key, "BOOLEAN");
                return Boolean.parseBoolean(configValue.toString());
            }
        } catch (ErrorValue e) {
            log.error("Error while reading config boolean key : " + key, e);
        }
        String value = lookUpEnvVariable(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;

    }

    /**
     * Retrieves the specified configuration value as a float.
     *
     * @param key          The configuration to be retrieved.
     * @param defaultValue The default value to be use in case there is no mapping for the provided key.
     * @return Configuration value mapped by the key.
     */
    public static float getAsFloat(String key, float defaultValue) {
        try {
            boolean contains = Contains.contains(key);
            if (contains) {
                Object configValue = GetConfig.get(key, "FLOAT");
                return Float.parseFloat(configValue.toString());
            }
        } catch (ErrorValue e) {
            log.error("Error while reading config boolean key : " + key, e);
        }
        String value = lookUpEnvVariable(key);
        return value != null ? Float.parseFloat(value) : defaultValue;

    }

    /**
     * Retrieves the specified configuration value as a map. If there is no mapping, an empty map will be returned.
     *
     * @param key The configuration to be retrieved.
     * @return Configuration value mapped by the key.
     */
    public static Map<String, String> getAsMap(String key) {
        try {
            MapValue configValue = (MapValue) GetConfig.get(key, "MAP");
            return InterceptorUtils.convertBMapToMap(configValue);
        } catch (ErrorValue e) {
            log.error("Error while reading config boolean key : " + key, e);
            return new HashMap<>();
        }
    }

    /**
     * Retrieves the specified configuration value as an array. If there is no mapping, an empty array will be returned.
     *
     * @param key The configuration to be retrieved.
     * @return Configuration value mapped by the key.
     */
    public static List<Map<String, String>> getAsList(String key) {
        try {
            ArrayValueImpl configValue = (ArrayValueImpl) GetConfig.get(key, "ARRAY");
            return InterceptorUtils.convertArrayValueToList(configValue);
        } catch (ErrorValue e) {
            log.error("Error while reading config boolean key : " + key, e);
            return new ArrayList<>();
        }
    }

    private static String lookUpEnvVariable(String key) {
        key = key.replaceAll(".", "_");
        return System.getenv(key);
    }
}
