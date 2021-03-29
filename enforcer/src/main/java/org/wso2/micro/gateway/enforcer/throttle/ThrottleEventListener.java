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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.micro.gateway.enforcer.throttle.dto.ThrottleCondition;
import org.wso2.micro.gateway.enforcer.throttle.utils.ThrottleUtils;

import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;

/**
 * JMS event listener for throttle data.
 */
public class ThrottleEventListener implements MessageListener {
    private static final Logger log = LogManager.getLogger(ThrottleEventListener.class);

    // These patterns will be used to determine for which type of keys the throttling condition has occurred.
    private final Pattern apiPattern = Pattern.compile("/.*/(.*):\\1_(condition_(\\d*)|default)");
    private static final int API_PATTERN_GROUPS = 3;
    private static final int API_PATTERN_CONDITION_INDEX = 2;

    private final Pattern resourcePattern = Pattern.compile("/.*/(.*)/\\1(.*)?:[A-Z]{0,7}_(condition_(\\d*)|default)");
    public static final int RESOURCE_PATTERN_GROUPS = 4;
    public static final int RESOURCE_PATTERN_CONDITION_INDEX = 3;

    @Override
    public void onMessage(Message message) {
        if (message == null) {
            log.warn("Dropping the empty/null event received through jms receiver");
            return;
        } else if (!(message instanceof MapMessage)) {
            log.warn("Event dropped due to unsupported message type " + message.getClass());
            return;
        }

        try {
            Topic jmsDestination = (Topic) message.getJMSDestination();
            MapMessage mapMessage = (MapMessage) message;
            Map<String, Object> map = new HashMap<>();
            Enumeration enumeration = mapMessage.getMapNames();
            while (enumeration.hasMoreElements()) {
                String key = (String) enumeration.nextElement();
                map.put(key, mapMessage.getObject(key));
            }

            if (ThrottleConstants.TOPIC_THROTTLE_DATA.equalsIgnoreCase(jmsDestination.getTopicName())) {
                if (map.get(ThrottleConstants.THROTTLE_KEY) != null) {
                    /*
                     * This message contains throttle data in map which contains Keys
                     * throttleKey - Key of particular throttling level
                     * isThrottled - Whether message has throttled or not
                     * expiryTimeStamp - When the throttling time window will expires
                     */

                    handleThrottleUpdateMessage(map);
                } else if (map.get(ThrottleConstants.POLICY_TEMPLATE_KEY) != null) {
                    /*
                     * This message contains key template data
                     * keyTemplateValue - Value of key template
                     * keyTemplateState - whether key template active or not
                     */
                    handleKeyTemplateMessage(map);
                } else if (map.get(ThrottleConstants.BLOCKING_CONDITION_KEY) != null) {
                    /*
                     * This message contains blocking condition data
                     * blockingCondition - Blocking condition type
                     * conditionValue - blocking condition value
                     * state - State whether blocking condition is enabled or not
                     */
                    handleBlockingMessage(map);
                }
            }
        } catch (JMSException e) {
            log.error("Error occurred when processing the received message ", e);
        }
    }

    private void handleThrottleUpdateMessage(Map<String, Object> map) {
        String throttleKey = map.get(ThrottleConstants.THROTTLE_KEY).toString();
        String throttleState = map.get(ThrottleConstants.IS_THROTTLED).toString();
        long timeStamp = Long.parseLong(map.get(ThrottleConstants.EXPIRY_TIMESTAMP).toString());
        Object evaluatedConditionObject = map.get(ThrottleConstants.EVALUATED_CONDITIONS);
        ThrottleDataHolder dataHolder = ThrottleDataHolder.getInstance();

        if (log.isDebugEnabled()) {
            log.debug("Received event -  throttleKey : " + throttleKey + ", isThrottled: " +
                    throttleState + ", expiryTime: " + new Date(timeStamp).toString());
        }

        if (ThrottleConstants.TRUE.equalsIgnoreCase(throttleState)) {
            dataHolder.addThrottleData(throttleKey, timeStamp);
            APICondition extractedKey = extractAPIorResourceKey(throttleKey);
            log.debug("Adding throttling key : {}",  extractedKey);

            if (extractedKey != null) {
                if (evaluatedConditionObject != null) {
                    String conditionStr = (String) evaluatedConditionObject;
                    List<ThrottleCondition> conditions = ThrottleUtils.extractThrottleCondition(conditionStr);
                    dataHolder.addThrottledConditions(extractedKey.getResourceKey(), extractedKey.getName(),
                            conditions);
                }
            }
        } else {
            dataHolder.removeThrottleData(throttleKey);
            APICondition extractedKey = extractAPIorResourceKey(throttleKey);
            if (extractedKey != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Removing throttling key : " + extractedKey.getResourceKey());
                }

                dataHolder.removeThrottledConditions(extractedKey.getResourceKey(), extractedKey.getName());
            }
        }
    }

    private synchronized void handleKeyTemplateMessage(Map<String, Object> map) {
        String keyTemplateValue = map.get(ThrottleConstants.POLICY_TEMPLATE_KEY).toString();
        String keyTemplateState = map.get(ThrottleConstants.TEMPLATE_KEY_STATE).toString();
        ThrottleDataHolder dataHolder = ThrottleDataHolder.getInstance();
        log.debug("Received Key - KeyTemplate: {}:{}", keyTemplateValue, keyTemplateState);

        if (ThrottleConstants.ADD.equals(keyTemplateState)) {
            dataHolder.addKeyTemplate(keyTemplateValue, keyTemplateValue);
        } else {
            dataHolder.removeKeyTemplate(keyTemplateValue);
        }
    }

    /**
     * Synchronized due to blocking data contains or not can updated by multiple threads.
     * Will not be a performance issue as this will not happen more frequently.
     */
    private synchronized void handleBlockingMessage(Map<String, Object> map) {
        log.debug("Received Key -  blockingCondition: {}, conditionValue: {}, tenantDomain: {}",
                map.get(ThrottleConstants.BLOCKING_CONDITION_KEY).toString(),
                map.get(ThrottleConstants.BLOCKING_CONDITION_VALUE).toString(),
                map.get(ThrottleConstants.BLOCKING_CONDITION_DOMAIN));

        ThrottleDataHolder dataHolder = ThrottleDataHolder.getInstance();
        String condition = map.get(ThrottleConstants.BLOCKING_CONDITION_KEY).toString();
        String conditionValue = map.get(ThrottleConstants.BLOCKING_CONDITION_VALUE).toString();
        String conditionState = map.get(ThrottleConstants.BLOCKING_CONDITION_STATE).toString();
        int conditionId = (int) map.get(ThrottleConstants.BLOCKING_CONDITION_ID);
        String tenantDomain = map.get(ThrottleConstants.BLOCKING_CONDITION_DOMAIN).toString();

        final boolean isIpBlockingCondition = ThrottleConstants.BLOCKING_CONDITIONS_IP.equals(condition) ||
                ThrottleConstants.BLOCK_CONDITION_IP_RANGE.equals(condition);
        if (ThrottleConstants.TRUE.equals(conditionState)) {
            if (isIpBlockingCondition) {
                dataHolder.addIpBlockingCondition(tenantDomain, conditionId, conditionValue, condition);
            } else {
                dataHolder.addBlockingCondition(conditionValue, conditionValue);
            }
        } else {
            if (isIpBlockingCondition) {
                dataHolder.removeIpBlockingCondition(tenantDomain, conditionId);
            } else {
                dataHolder.removeBlockingCondition(conditionValue);
            }
        }
    }

    private APICondition extractAPIorResourceKey(String throttleKey) {
        Matcher m = resourcePattern.matcher(throttleKey);
        if (m.matches()) {
            if (m.groupCount() == RESOURCE_PATTERN_GROUPS) {
                String condition = m.group(RESOURCE_PATTERN_CONDITION_INDEX);
                String resourceKey = throttleKey.substring(0, throttleKey.indexOf("_" + condition));
                return new APICondition(resourceKey, condition);
            }
        } else {
            m = apiPattern.matcher(throttleKey);
            if (m.matches()) {
                if (m.groupCount() == API_PATTERN_GROUPS) {
                    String condition = m.group(API_PATTERN_CONDITION_INDEX);
                    String resourceKey = throttleKey.substring(0, throttleKey.indexOf("_" + condition));
                    return new APICondition(resourceKey, condition);
                }
            }
        }
        return null;
    }
}
