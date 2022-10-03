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

package org.wso2.choreo.connect.enforcer.throttle;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.discovery.throttle.IPCondition;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.config.ConfigHolder;
import org.wso2.choreo.connect.enforcer.config.dto.ThrottleConfigDto;
import org.wso2.choreo.connect.enforcer.discovery.ThrottleDataDiscoveryClient;
import org.wso2.choreo.connect.enforcer.throttle.dto.Decision;
import org.wso2.choreo.connect.enforcer.throttle.dto.IPRange;
import org.wso2.choreo.connect.enforcer.throttle.dto.ThrottleCondition;
import org.wso2.choreo.connect.enforcer.throttle.utils.ThrottleUtils;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class holds throttle data per given node. In addition to holding throttle data if provides
 * required methods to evaluate throttle state of a given throttling key.
 */
public class ThrottleDataHolder {
    private static final Logger log = LogManager.getLogger(ThrottleDataHolder.class);

    private final Map<String, Long> throttleDecisions;
    private Map<String, String> keyTemplates;
    private Map<String, String> blockedConditions;
    private Map<String, Set<IPRange>> blockedIpConditions;
    private static ThrottleDataHolder instance;
    private final Map<String, Map<String, List<ThrottleCondition>>> conditionData = new ConcurrentHashMap<>();

    private ThrottleDataHolder() {
        this.throttleDecisions = new ConcurrentHashMap<>();
        this.keyTemplates = new ConcurrentHashMap<>();
        this.blockedConditions = new ConcurrentHashMap<>();
        this.blockedIpConditions = new ConcurrentHashMap<>();
    }

    public static ThrottleDataHolder getInstance() {
        if (instance == null) {
            instance = new ThrottleDataHolder();
        }

        return instance;
    }

    /**
     * Load initial data maps from throttle data endpoints.
     */
    public void init() {
        ThrottleDataDiscoveryClient.getInstance().watchThrottleData();
    }

    /**
     * Add all key templates in a given map to the key template map.
     *
     * @param templates Map of key template
     */
    public void addKeyTemplates(Map<String, String> templates) {
        if (templates == null || templates.size() < 1) {
            keyTemplates = new ConcurrentHashMap<>();
        }
        keyTemplates = new ConcurrentHashMap<>(templates);
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

        if (conditionData.containsKey(key)) {
            conditionMap = conditionData.get(key);
        } else {
            conditionMap = new ConcurrentHashMap<>();
            conditionData.put(key, conditionMap);
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
        if (conditionData.containsKey(key)) {
            Map<String, List<ThrottleCondition>> conditionMap = conditionData.get(key);
            conditionMap.remove(conditionKey);
            if (conditionMap.isEmpty()) {
                conditionData.remove(key);
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
        throttleDecisions.put(key, timestamp);
    }

    /**
     * Remove throttle data item from the throttle data map.
     *
     * @param key throttle key to be removed
     */
    public void removeThrottleData(String key) {
        throttleDecisions.remove(key);
    }


    /**
     * Add all blocking conditions in a {@link List<String>} definition.
     *
     * @param conditions a blocking condition dto with all types of conditions to add.
     */
    public void addBlockingConditions(List<String> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            blockedConditions = new ConcurrentHashMap<>();
            return;
        }
        Map<String, String> conditionMap = FilterUtils.generateMap(conditions);
        blockedConditions = new ConcurrentHashMap<>(conditionMap);
    }

    public void addIpBlockingConditions(List<IPCondition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            blockedIpConditions = new ConcurrentHashMap<>();
            return;
        }
        Map<String, Set<IPRange>> newConditions = new ConcurrentHashMap<>();
        for (IPCondition condition : conditions) {
            Set<IPRange> ipRanges = newConditions.get(condition.getTenantDomain());
            if (ipRanges == null) {
                ipRanges = new HashSet<>();
            }

            IPRange ipRange = new IPRange();
            if (ThrottleConstants.BLOCK_CONDITION_IP_RANGE.equals(condition.getType())) {
                ipRange.setStartingIP(condition.getStartingIp());
                ipRange.setEndingIp(condition.getEndingIp());
            } else if (ThrottleConstants.BLOCKING_CONDITIONS_IP.equals(condition.getType())) {
                ipRange.setFixedIp(condition.getFixedIp());
            }
            ipRange.setId(condition.getId());
            ipRange.setTenantDomain(condition.getTenantDomain());
            ipRange.setType(condition.getType());
            ipRange.setInvert(condition.getInvert());
            ipRange.setStartingIpBigIntValue(FilterUtils.ipToBigInteger(condition.getStartingIp()));
            ipRange.setEndingIpBigIntValue(FilterUtils.ipToBigInteger(condition.getEndingIp()));

            ipRanges.add(ipRange);
            newConditions.put(condition.getTenantDomain(), ipRanges);
        }

        blockedIpConditions = newConditions;
    }

    /**
     * Checks if there are any blocking conditions received from the traffic manager.
     *
     * @return returns {@code true} if blocking conditions are available {@code false} otherwise.
     */
    public boolean isBlockingConditionsPresent() {
        return (blockedConditions.size() > 0 || blockedIpConditions.size() > 0);
    }

    /**
     * Checks if a request is blocked by given blocking keys.
     *
     * @param apiBlockingKey blocking key for API blocking
     * @param applicationBlockingKey blocking key for Application blocking
     * @param userBlockingKey blocking key for User blocking
     * @param ipBlockingKey blocking key for IP blocking
     * @param subscriptionBlockingKey blocking key for Subscription blocking
     * @param apiTenantDomain tenant domain of the current request
     * @return {@code true} if request is blocked by any of the conditions, {@code false} otherwise
     */
    public boolean isRequestBlocked(String apiBlockingKey, String applicationBlockingKey, String userBlockingKey,
                                    String ipBlockingKey, String subscriptionBlockingKey, String apiTenantDomain) {
        return (blockedConditions.containsKey(apiBlockingKey)
                || blockedConditions.containsKey(applicationBlockingKey)
                || blockedConditions.containsKey(userBlockingKey)
                || blockedConditions.containsKey(subscriptionBlockingKey)
                || isIpLevelBlocked(apiTenantDomain, ipBlockingKey));
    }
    /**
     * This method will check given key in throttle data Map. A key is considered throttled if,
     * <ol>
     *     <li>A values for the given @{code key} exists in the throttle data map</li>
     *     <li>Validity timestamp for the provided key is not passed already</li>
     * </ol>
     *
     * @param key throttle key
     * @return throttle {@link Decision} defining the whether request is throttled or not
     */
    public Decision isThrottled(String key) {
        Decision decision = new Decision();
        Long timestamp = this.throttleDecisions.get(key);

        if (timestamp != null) {
            long currentTime = System.currentTimeMillis();
            decision.setThrottled(true);
            decision.setResetAt(timestamp);

            if (timestamp < currentTime) {
                this.throttleDecisions.remove(key);
                decision.setThrottled(false);
            }
        }

        return decision;
    }

    /**
     * Decide whether {@code key} is throttled or not by the traffic manager.
     * This function is defined to evaluate only API and Resource level throttling
     * decisions received from the traffic manager.
     *
     * @param key     throttle key to evaluate the throttle decision
     * @param context RequestContext of the evaluated request
     * @return throttle {@link Decision}
     */
    public Decision isAdvancedThrottled(String key, RequestContext context) {
        String conditionKey = null;
        Decision decision = new Decision();
        Map<String, List<ThrottleCondition>> conditionGrps = conditionData.get(key);
        List<ThrottleCondition> defaultGrp = null;

        if (conditionGrps == null) {
            return decision;
        }

        log.debug("Found throttle condition in condition map");
        // iterate through all available conditions and find if the current request
        // attributes are eligible to be throttled by the available throttled conditions
        for (String name : conditionGrps.keySet()) {
            if (!ThrottleConstants.DEFAULT_THROTTLE_CONDITION.equals(name)) {
                boolean isPipelineThrottled = isThrottledByCondition(conditionGrps.get(name), context);
                if (isPipelineThrottled) {
                    conditionKey = name;
                    break;
                }
            } else {
                defaultGrp = conditionGrps.get(name);
            }
        }

        if (conditionKey == null && defaultGrp != null) {
            boolean isPipelineThrottled = isThrottledByCondition(defaultGrp, context);
            if (!isPipelineThrottled) {
                conditionKey = ThrottleConstants.DEFAULT_THROTTLE_CONDITION;
            }
        }

        // if we detect the request is throttled by a condition. Then check the validity of throttle
        // decision from the throttle event data available in the throttleDataMap
        if (conditionKey != null) {
            log.debug("Found throttled pipeline with condition: {}", conditionKey);
            String combinedThrottleKey = key + '_' + conditionKey;

            // if throttle data is not available for the combined key, conditional throttle decision
            // is no longer valid
            Long timestamp = throttleDecisions.get(combinedThrottleKey);
            if (timestamp == null) {
                return decision;
            }

            long currentTime = System.currentTimeMillis();
            if (timestamp < currentTime) {
                this.throttleDecisions.remove(key);
                this.conditionData.remove(key);
                return decision;
            }

            decision.setThrottled(true);
            decision.setResetAt(timestamp);
            return decision;
        }

        return decision;
    }

    /**
     * Check if the request is throttled by an advanced throttle condition.
     * Such as IP, header, query param based conditions.
     *
     * @param conditions throttled conditions received from global throttle engine
     * @param req        RequestContext containing all required information about the evaluated request.
     * @return {@code true} if throttled by a condition, {@code false} otherwise
     */
    private boolean isThrottledByCondition(List<ThrottleCondition> conditions, RequestContext req) {
        boolean isThrottled = false;
        ThrottleConfigDto conf = ConfigHolder.getInstance().getConfig().getThrottleConfig();

        for (ThrottleCondition condition : conditions) {
            // We initially set throttled flag to true. Then we move onto evaluating all conditions and
            // set the flag to false accordingly. This is done in this way to implement the `AND` logic
            // between each condition inside a condition group.
            isThrottled = true;
            ThrottleCondition.HeaderConditions headerConditions = condition.getHeaderConditions();
            ThrottleCondition.IPCondition ipCondition = condition.getIpCondition();
            ThrottleCondition.IPCondition ipRangeCondition = condition.getIpRangeCondition();
            ThrottleCondition.QueryParamConditions queryConditions = condition.getQueryParameterConditions();
            ThrottleCondition.JWTClaimConditions claimConditions = condition.getJwtClaimConditions();

            if (ipCondition != null) {
                if (!isMatchingIp(req.getClientIp(), ipCondition)) {
                    isThrottled = false;
                }
            } else if (ipRangeCondition != null) {
                if (!isWithinIpRange(req.getClientIp(), ipRangeCondition)) {
                    isThrottled = false;
                }
            }
            if (conf.isHeaderConditionsEnabled()
                    && headerConditions != null && !headerConditions.getValues().isEmpty()) {
                if (!isHeaderPresent(req, headerConditions)) {
                    isThrottled = false;
                }
            }
            if (conf.isQueryConditionsEnabled()
                    && queryConditions != null && !queryConditions.getValues().isEmpty()) {
                if (!isQueryParamPresent(req, queryConditions)) {
                    isThrottled = false;
                }
            }
            if (conf.isJwtClaimConditionsEnabled()
                    && (claimConditions != null && !claimConditions.getValues().isEmpty())) {
                Map<String, String> c = ThrottleUtils.getJWTClaims(req.getAuthenticationContext().getCallerToken());
                if (c == null || !isJwtClaimPresent(c, claimConditions)) {
                    isThrottled = false;
                }
            }

            if (isThrottled) {
                break;
            }
        }

        return isThrottled;
    }

    /**
     * Verify if the request is throttled by a custom key template policy.
     * This method call is an expensive operation and should not enabled by default.
     * If we enabled this policy then all APIs available in system will have
     * to go through this check.
     *
     * @return throttle {@link Decision}
     */
    public Decision isThrottledByCustomPolicy(String userID, String resourceKey, String apiContext, String apiVersion,
                                             String appTenant, String apiTenant, int appId, String clientIp,
                                              String customPropertyString) {
        Decision decision = new Decision();
        if (keyTemplates.size() > 0) {
            for (String key : keyTemplates.keySet()) {
                key = key.replaceAll("\\$resourceKey", resourceKey);
                key = key.replaceAll("\\$userId", userID);
                key = key.replaceAll("\\$apiContext", apiContext);
                key = key.replaceAll("\\$apiVersion", apiVersion);
                key = key.replaceAll("\\$appTenant", appTenant);
                key = key.replaceAll("\\$apiTenant", apiTenant);
                key = key.replaceAll("\\$appId", String.valueOf(appId));

                if (clientIp != null) {
                    key = key.replaceAll("\\$clientIp", FilterUtils.ipToBigInteger(clientIp).toString());
                }

                // If custom throttle properties exist, populate the key template with custom properties.
                if (!customPropertyString.equals("null")) {
                    String[] customPropertyList = customPropertyString.split(" ");
                    for (String customProperty: customPropertyList) {
                        String[] propertyPair = customProperty.split("=");
                        if (propertyPair.length == 2) {
                            key = key.replaceAll("\\$customProperty." + propertyPair[0], propertyPair[1]);
                        } else {
                            log.debug("Invalid custom property string : {}", customProperty);
                        }
                    }
                }

                decision = isThrottled(key);
                if (decision.isThrottled()) {
                    return decision;
                }
            }
        }

        return decision;
    }

    private boolean isIpLevelBlocked(String apiTenantDomain, String ip) {
        Set<IPRange> ipRanges = blockedIpConditions.get(apiTenantDomain);

        if (ipRanges != null && ipRanges.size() > 0) {
            log.debug("Tenant {} contains block conditions", apiTenantDomain);
            for (IPRange ipRange : ipRanges) {
                if (ThrottleConstants.BLOCKING_CONDITIONS_IP.equals(ipRange.getType())) {
                    if (ip.equals(ipRange.getFixedIp())) {
                        if (!ipRange.isInvert()) {
                            log.debug("Blocked IP detected");
                            return true;
                        }
                    } else {
                        if (ipRange.isInvert()) {
                            log.debug("Blocked IP detected");
                            return true;
                        }
                    }
                } else if (ThrottleConstants.BLOCK_CONDITION_IP_RANGE.equals(ipRange.getType())) {
                    BigInteger ipBigIntegerValue = FilterUtils.ipToBigInteger(ip);

                    if (((ipBigIntegerValue.compareTo(ipRange.getStartingIpBigIntValue()) > 0) &&
                            (ipBigIntegerValue.compareTo(ipRange.getEndingIpBigIntValue()) < 0))) {
                        if (!ipRange.isInvert()) {
                            log.debug("Blocked IP detected in an IP Range");
                            return true;
                        }
                    } else {
                        if (ipRange.isInvert()) {
                            log.debug("Blocked IP detected in an IP Range");
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isMatchingIp(String clientIp, ThrottleCondition.IPCondition ipCondition) {
        BigInteger longIp = FilterUtils.ipToBigInteger(clientIp);
        boolean isMatched = (longIp.equals(ipCondition.getSpecificIp()));

        if (ipCondition.isInvert()) {
            return !isMatched;
        }

        return isMatched;
    }

    private boolean isWithinIpRange(String clientIp, ThrottleCondition.IPCondition ipCondition) {
        boolean status;

        if (StringUtils.isEmpty(clientIp)) {
            return false;
        }

        BigInteger currentIp = FilterUtils.ipToBigInteger(clientIp);
        status = ipCondition.getStartingIp().compareTo(currentIp) <= 0
                && ipCondition.getEndingIp().compareTo(currentIp) >= 0;

        if (ipCondition.isInvert()) {
            return !status;
        }

        return status;
    }

    private boolean isHeaderPresent(RequestContext req, ThrottleCondition.HeaderConditions conditions) {
        boolean status = true;
        Map<String, String> headers = req.getHeaders();

        for (Map.Entry<String, String> entry : conditions.getValues().entrySet()) {
            if (headers != null) {
                String value = headers.get(StringUtils.lowerCase(entry.getKey()));

                if (StringUtils.isEmpty(value)) {
                    status = false;
                    break;
                }
                Pattern pattern = Pattern.compile(entry.getValue());
                Matcher matcher = pattern.matcher(value);
                status = status && matcher.find();
            }
        }

        if (conditions.isInvert()) {
            return !status;
        }

        return status;
    }

    private boolean isJwtClaimPresent(Map<String, String> claims, ThrottleCondition.JWTClaimConditions conditions) {
        boolean status = true;

        for (Map.Entry<String, String> jwtClaim : conditions.getValues().entrySet()) {
            String value = claims.get(jwtClaim.getKey());
            if (value == null) {
                status = false;
                break;
            }

            Pattern pattern = Pattern.compile(jwtClaim.getValue());
            Matcher matcher = pattern.matcher(value);
            status = status && matcher.find();
        }

        if (conditions.isInvert()) {
            return !status;
        }

        return status;
    }

    private boolean isQueryParamPresent(RequestContext req, ThrottleCondition.QueryParamConditions condition) {
        Map<String, String> queryParamMap = req.getQueryParameters();
        boolean status = true;

        for (Map.Entry<String, String> queryParam : condition.getValues().entrySet()) {
            String value = queryParamMap.get(queryParam.getKey());
            if (value == null) {
                status = false;
                break;
            }

            Pattern pattern = Pattern.compile(queryParam.getValue());
            Matcher matcher = pattern.matcher(value);
            status = status && matcher.find();
        }

        if (condition.isInvert()) {
            return !status;
        }

        return status;
    }
}
