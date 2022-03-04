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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.commons.logging.ErrorDetails;
import org.wso2.choreo.connect.enforcer.commons.logging.LoggingConstants;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.throttle.dto.ThrottleCondition;
import org.wso2.choreo.connect.enforcer.throttle.utils.ThrottleUtils;

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
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
        } else if (!(message instanceof TextMessage)) {
            log.warn("Event dropped due to unsupported message type " + message.getClass());
            return;
        }

        try {
            Topic jmsDestination = (Topic) message.getJMSDestination();
            String textMessage = ((TextMessage) message).getText();
            JsonNode payloadData = new ObjectMapper().readTree(textMessage).path(APIConstants.EVENT_PAYLOAD).
                    path(APIConstants.EVENT_PAYLOAD_DATA);

            if (ThrottleConstants.TOPIC_THROTTLE_DATA.equalsIgnoreCase(jmsDestination.getTopicName())) {
                if (payloadData.get(ThrottleConstants.THROTTLE_KEY) != null) {
                    /*
                     * This message contains throttle data in map which contains Keys
                     * throttleKey - Key of particular throttling level
                     * isThrottled - Whether message has throttled or not
                     * expiryTimeStamp - When the throttling time window will expires
                     */

                    handleThrottleUpdateMessage(payloadData);
                }
            }
        } catch (JMSException | JsonProcessingException e) {
            log.error("Error occurred when processing the received message ",
                    ErrorDetails.errorLog(LoggingConstants.Severity.MAJOR, 6602), e);
        }
    }

    private void handleThrottleUpdateMessage(JsonNode msg) {
        String throttleKey = msg.get(ThrottleConstants.THROTTLE_KEY).asText();
        String throttleState = msg.get(ThrottleConstants.IS_THROTTLED).asText();
        long timeStamp = Long.parseLong(msg.get(ThrottleConstants.EXPIRY_TIMESTAMP).asText());
        Object evaluatedConditionObject = msg.get(ThrottleConstants.EVALUATED_CONDITIONS);
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
                    String conditionStr = evaluatedConditionObject.toString();
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
