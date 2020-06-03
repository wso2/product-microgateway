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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Utility methods used for global throttling scenarios.
 */
public class ThrottleUtils {

    private static final Logger log = LoggerFactory.getLogger("ballerina");
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
}
