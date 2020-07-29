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

package org.wso2.micro.gateway.core.throttle.global;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods used for global throttling scenarios.
 */
public class ThrottleUtils {

    private static final Logger log = LogManager.getLogger(ThrottleUtils.class);
    private static final int API_PATTERN_GROUPS = 3;
    private static final int API_PATTERN_CONDITION_INDEX = 2;
    private static final int RESOURCE_PATTERN_GROUPS = 4;
    private static final int RESOURCE_PATTERN_CONDITION_INDEX = 3;

    // These patterns will be used to determine for which type of keys the throttling condition has occurred.
    private static Pattern apiPattern = Pattern.compile("/.*/(.*):\\1_(condition_(\\d*)|default)");
    private static Pattern resourcePattern = Pattern.compile("/.*/(.*)/\\1(.*)?:[A-Z]{0,6}_(condition_(\\d*)|default)");

    /**
     * This method provides the BigInteger string value for the given IP address.
     * This supports both IPv4 and IPv6 address.
     * @param ipAddress ip address.
     * @return BigInteger string value for the given ip address. returns 0 for unknown host.
     */
    public static String ipToBigInteger(String ipAddress) {
        InetAddress address;
        try {
            address = InetAddress.getByName(ipAddress);
            byte[] bytes = address.getAddress();
            return new BigInteger(1, bytes).toString();
        } catch (UnknownHostException e) {
            //ignore the error and log it
            log.error("Error while parsing host IP " + ipAddress, e);
        }
        return BigInteger.ZERO.toString();
    }

    /**
     * This method checks whether the given IP address inside a certain IP address range.
     * This supports both IPv4 and IPv6 address.
     * @param ip ip address to be verified.
     * @param startingIP starting IP of the address range.
     * @param endingIp ending IP of the address range.
     * @return true if IP address is inside the given range.
     */
    public static boolean isIpWithinRange(String ip, String startingIP, String endingIp) {
        BigInteger ipNumber = new BigInteger(ipToBigInteger(ip));
        BigInteger startingIpNumber = new BigInteger(startingIP);
        BigInteger endingIpNumber = new BigInteger(endingIp);
        return (ipNumber.compareTo(startingIpNumber) > 0) && (ipNumber.compareTo(endingIpNumber) < 0);
    }

    public static String extractAPIorResourceKey(String throttleKey) {
        Matcher m = resourcePattern.matcher(throttleKey);
        if (m.matches()) {
            if (m.groupCount() == RESOURCE_PATTERN_GROUPS) {
                String condition = m.group(RESOURCE_PATTERN_CONDITION_INDEX);
                String resourceKey = throttleKey.substring(0, throttleKey.indexOf("_" + condition));
                return "{\"resourceKey\":\"" + resourceKey + "\", \"name\":\"" + condition + "\"}";
            }
        } else {
            m = apiPattern.matcher(throttleKey);
            if (m.matches()) {
                if (m.groupCount() == API_PATTERN_GROUPS) {
                    String condition = m.group(API_PATTERN_CONDITION_INDEX);
                    String resourceKey = throttleKey.substring(0, throttleKey.indexOf("_" + condition));
                    return "{\"resourceKey\":\"" + resourceKey + "\", \"name\":\"" + condition + "\"}";
                }
            }
        }

        return null;
    }

    public static boolean isPatternMatched(String pattern, String match) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(match);
        return m.find();
    }
}
