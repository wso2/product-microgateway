/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.micro.gateway.enforcer.throttle;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class holds throttle data per given node. In addition to holding throttle data if provides
 * required methods to evaluate throttle state of a given throttling key.
 */
public class ThrottleDataHolder {
    private static final Log log = LogFactory.getLog(ThrottleDataHolder.class);

    private final Map<String, Long> throttleDataMap;
    private static ThrottleDataHolder instance;
    private final Map<String, Map<String, List<ThrottleCondition>>> conditionDtoMap = new ConcurrentHashMap<>();

    private ThrottleDataHolder() {
        throttleDataMap = new ConcurrentHashMap<>();
    }

    public static ThrottleDataHolder getInstance() {
        if (instance == null) {
            instance = new ThrottleDataHolder();
        }

        return instance;
    }

    /**
     * Add throttle conditions to the Throttle Conditions map.
     *
     * @param key            throttle key
     * @param conditionKey   condition key
     * @param conditionValue conditions to be added to the map
     */
    public void addThrottledConditions(String key, String conditionKey, List<ThrottleCondition> conditionValue) {
        Map<String, List<ThrottleCondition>> conditionMap;

        if (conditionDtoMap.containsKey(key)) {
            conditionMap = conditionDtoMap.get(key);
        } else {
            conditionMap = new ConcurrentHashMap<>();
            conditionDtoMap.put(key, conditionMap);
        }
        if (!conditionMap.containsKey(conditionKey)) {
            conditionMap.put(conditionKey, conditionValue);
        }
    }

    /**
     * Remove throttle conditions from the Throttle Conditions map.
     *
     * @param key          throttle key
     * @param conditionKey condition key to be removed
     */
    public void removeThrottledConditions(String key, String conditionKey) {
        if (conditionDtoMap.containsKey(key)) {
            Map<String, List<ThrottleCondition>> conditionMap = conditionDtoMap.get(key);
            conditionMap.remove(conditionKey);
            if (conditionMap.isEmpty()) {
                conditionDtoMap.remove(key);
            }
        }
    }

    /**
     * Add new throttle data item to the throttle data map.
     *
     * @param key       throttle key to be added
     * @param timestamp throttle timestamp
     */
    public void addThrottleData(String key, Long timestamp) {
        throttleDataMap.put(key, timestamp);
    }

    /**
     * Remove throttle data item from the throttle data map.
     *
     * @param key throttle key to be removed
     */
    public void removeThrottleData(String key) {
        throttleDataMap.remove(key);
    }

    /**
     * This method will check given key in throttle data Map. A key is considered throttled if,
     * <ol>
     *     <li>A values for the given @{code key} exists in the throttle data map</li>
     *     <li>Validity timestamp for the provided key is not passed already</li>
     * </ol>
     *
     * @param key throttle key
     * @return {@code true} if event is throttled {@code false} if event is not throttled.
     */
    public boolean isThrottled(String key) {
        boolean isThrottled = this.throttleDataMap.containsKey(key);

        if (isThrottled) {
            long currentTime = System.currentTimeMillis();
            long timestamp = this.throttleDataMap.get(key);
            if (timestamp < currentTime) {
                this.throttleDataMap.remove(key);
                isThrottled = false;
            }
        }

        return isThrottled;
    }

}
