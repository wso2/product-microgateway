/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.enforcer.throttle.databridge.agent.util;

import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.conf.DataEndpointConfiguration;
import org.wso2.choreo.connect.enforcer.throttle.databridge.agent.exception.DataEndpointConfigurationException;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Util class which holds all the utility processing methods in the data publisher operation.
 */
public class DataPublisherUtil {

    /**
     * Making as private to avoid the instantiation of the class.
     */
    private DataPublisherUtil() {}

    /**
     * Process and extracts the receiver Groups from the string of URL set pattern passed in.
     *
     * @param urlSet receiverGroups URL set
     * @return List of Receiver groups
     * @throws DataEndpointConfigurationException
     */
    public static ArrayList<Object[]> getEndpointGroups(String urlSet)
            throws DataEndpointConfigurationException {
        ArrayList<String> urlGroups = new ArrayList<String>();
        ArrayList<Object[]> endPointGroups = new ArrayList<Object[]>();
        Pattern regex = Pattern.compile("\\{.*?\\}");
        Matcher regexMatcher = regex.matcher(urlSet);
        while (regexMatcher.find()) {
            urlGroups.add(regexMatcher.group().replace("{", "").replace("}", ""));
        }
        if (urlGroups.size() == 0) {
            urlGroups.add(urlSet.replace("{", "").replace("}", ""));
        }
        for (String aURLGroup : urlGroups) {
            endPointGroups.add(getEndpoints(aURLGroup));
        }
        return endPointGroups;
    }

    /**
     * Returns an object array which has first element as boolean
     * for specify the URLs are fail over configuration or LB configuration.
     * From the 2nd element onwards the object array will hold the separated URLs
     *
     * @param aURLGroup one receiver group's URL
     * @return First element - boolean (isFailOver), next elements are each URLs
     * @throws DataEndpointConfigurationException
     */
    private static Object[] getEndpoints(String aURLGroup)
            throws DataEndpointConfigurationException {
        boolean isLBURL = false, isFailOverURL = false;
        String[] urls;
        if (aURLGroup.contains(",")) {
            isLBURL = true;
        }
        if (aURLGroup.contains("|")) {
            isFailOverURL = true;
        }

        if (isLBURL && isFailOverURL) {
            throw new DataEndpointConfigurationException("Invalid data endpoints URL set provided : " + aURLGroup
                    + ", a URL group can be configured as failover OR load balancing endpoints.");
        } else if (isLBURL) {
            urls = aURLGroup.split(DataEndpointConstants.LB_URL_GROUP_SEPARATOR);
        } else if (isFailOverURL) {
            urls = aURLGroup.split(DataEndpointConstants.FAILOVER_URL_GROUP_SEPARATOR_REGEX);
        } else {
            urls = new String[]{aURLGroup};
        }
        Object[] endpoint = new Object[urls.length + 1];
        endpoint[0] = isFailOverURL;
        for (int i = 0; i < urls.length; i++) {
            endpoint[i + 1] = urls[i].trim();
        }
        return endpoint;
    }

    /**
     * Validate whether the receiverGroup and authenticationGroups are matching with pattern.
     * Basically if the receiver groups has been configured to be in the failover pattern,
     * then the authentication URL also needs to be in the same way. Hence this method validates
     * the provided receiverGroups, and authenticationGroup.
     *
     * @param receiverGroups List of Receiver groups
     * @param authGroups     List of Authentication groups.
     * @throws DataEndpointConfigurationException
     */
    public static void validateURLs(ArrayList receiverGroups, ArrayList authGroups)
            throws DataEndpointConfigurationException {
        if (receiverGroups.size() == authGroups.size()) {
            for (int i = 0; i < receiverGroups.size(); i++) {
                Object[] receiverGroup = (Object[]) receiverGroups.get(i);
                Object[] authGroup = (Object[]) authGroups.get(i);

                if (receiverGroup.length == authGroup.length) {
                    boolean isFailOver = (Boolean) receiverGroup[0];
                    boolean isAuthFailOver = (Boolean) ((Object[]) receiverGroups.get(i))[0];
                    if (isFailOver != isAuthFailOver) {
                        throw new DataEndpointConfigurationException("Receiver and authentication URL group set " +
                                "doesn't match. Receiver URL group: " + getURLSet(receiverGroup)
                                + " is configured as failOver : " + isFailOver + ", but Authentication URL group: "
                                + getURLSet(authGroup) + " is configured as failOver :" + isAuthFailOver);
                    }
                } else {
                    throw new DataEndpointConfigurationException("Receiver and authentication URL group set " +
                            "doesn't match. Receiver URL group: " + getURLSet(receiverGroup) + ", " +
                            "but Authentication URL group: " + getURLSet(authGroup));
                }
            }
        } else {
            throw new DataEndpointConfigurationException("Receiver and authentication URL set doesn't match. " +
                    "Receiver URL groups: " + receiverGroups.size() + ", but Authentication URL groups: "
                    + authGroups.size());
        }
    }

    /**
     * Returns the URL set string based on the URLs provided. The first element of the array is
     * a boolean which indicates whether it's failover or load balancing.
     *
     * @param urlGroup Array of url and the first element of array should specify
     *                 whether its load balancing or fail over
     * @return String representation of URL set.
     */
    private static String getURLSet(Object[] urlGroup) {
        boolean isFailOver = (Boolean) urlGroup[0];
        StringBuilder urlSet = new StringBuilder("");
        for (int i = 1; i < urlGroup.length; i++) {
            urlSet.append(urlGroup[i]);
            if (i != urlGroup.length - 1) {
                if (isFailOver) {
                    urlSet.append(DataEndpointConstants.FAILOVER_URL_GROUP_SEPARATOR);
                } else {
                    urlSet.append(DataEndpointConstants.LB_URL_GROUP_SEPARATOR);
                }
            }
        }
        return urlSet.toString();
    }

    /**
     * Extracts and return the protocol, host, port elements from the URL.
     *
     * @param url String of URL that needs to be processed.
     * @return Array which has elements - protocol, host, and port in the given order.
     */
    public static String[] getProtocolHostPort(String url) throws DataEndpointConfigurationException {
        String[] keyElements = url.split(DataEndpointConstants.SEPARATOR);
        String[] urlElements = keyElements[0].split(":");
        if (urlElements.length != 3) {
            throw new DataEndpointConfigurationException("Invalid URL is provided :" + url +
                    ". Receiver URL property should take the format : " + "protocol://host:port");
        }
        return new String[]{urlElements[0], urlElements[1].replace("//", ""), urlElements[2]};
    }

    /**
     * Deduce the default authentication URL based on the receiver URL passed in.
     *
     * @param receiverURL receiver URL for which it's required to get the authentication URL.
     * @return default authentication URL.
     */
    public static String getDefaultAuthUrl(String receiverURL) throws DataEndpointConfigurationException {
        String[] urlElements = getProtocolHostPort(receiverURL);
        int port = Integer.parseInt(urlElements[2]);
        String host = urlElements[1];
        return DataEndpointConfiguration.Protocol.SSL.toString() + "://" + host + ":" +
                (port + DataEndpointConstants.DEFAULT_AUTH_PORT_OFFSET);
    }

    /**
     * Deduce the default authentication URL set based on the receiver URL set passed in.
     *
     * @param receiverURLSet receiver URL set for which it's required to get the authentication URL set.
     * @return default authentication URL set.
     */
    public static String getDefaultAuthURLSet(String receiverURLSet) throws DataEndpointConfigurationException {
        ArrayList<Object[]> receiverURLGroups = DataPublisherUtil.getEndpointGroups(receiverURLSet);
        StringBuilder authURLSet = new StringBuilder("");
        for (int i = 0; i < receiverURLGroups.size(); i++) {
            Object[] receiverGroup = receiverURLGroups.get(i);
            boolean failOver = (Boolean) receiverGroup[0];
            authURLSet.append("{");
            for (int j = 1; j < receiverGroup.length; j++) {
                authURLSet.append(DataPublisherUtil.getDefaultAuthUrl(receiverGroup[j].toString()));
                if (j != receiverGroup.length - 1) {
                    if (failOver) {
                        authURLSet.append(DataEndpointConstants.FAILOVER_URL_GROUP_SEPARATOR);
                    } else {
                        authURLSet.append(DataEndpointConstants.LB_URL_GROUP_SEPARATOR);
                    }
                }
            }
            authURLSet.append("}");
            if (i != receiverURLGroups.size() - 1) {
                authURLSet.append(",");
            }
        }
        return authURLSet.toString();
    }
}
