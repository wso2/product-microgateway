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

package org.wso2.choreo.connect.enforcer.throttle.utils;

import com.google.gson.Gson;
import com.nimbusds.jose.util.JSONObjectUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.wso2.choreo.connect.enforcer.commons.model.RequestContext;
import org.wso2.choreo.connect.enforcer.throttle.PolicyConstants;
import org.wso2.choreo.connect.enforcer.throttle.ThrottleConstants;
import org.wso2.choreo.connect.enforcer.throttle.ThrottleFilter;
import org.wso2.choreo.connect.enforcer.throttle.dto.ThrottleCondition;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * Utilities related to throttling.
 */
public class ThrottleUtils {

    private static final Logger log = LogManager.getLogger(ThrottleFilter.class);

    /**
     * Extract a {@code ThrottleCondition} from a provided compatible base64 encoded string.
     *
     * @param base64EncodedString base64 encoded string containing a json in compatible format
     * @return list of extracted {@code ThrottleCondition}s
     */
    public static List<ThrottleCondition> extractThrottleCondition(String base64EncodedString) {

        List<ThrottleCondition> conditionDtoList = new ArrayList<>();
        String base64Decoded = new String(Base64.decodeBase64(base64EncodedString));
        JSONTokener tokener = new JSONTokener(base64Decoded);
        JSONArray conditionJsonArray = new JSONArray(tokener);
        for (int index = 0; index < conditionJsonArray.length(); index++) {
            Object conditionJson = conditionJsonArray.get(index);
            ThrottleCondition conditionDto = new ThrottleCondition();
            JSONObject conditionJsonObject = (JSONObject) conditionJson;
            if (conditionJsonObject.has(PolicyConstants.IP_SPECIFIC_TYPE.toLowerCase())) {
                JSONObject ipSpecificCondition = (JSONObject) conditionJsonObject.get(PolicyConstants.IP_SPECIFIC_TYPE
                        .toLowerCase());
                ThrottleCondition.IPCondition ipCondition = new Gson().fromJson(ipSpecificCondition.toString(),
                        ThrottleCondition.IPCondition.class);
                conditionDto.setIpCondition(ipCondition);
            } else if (conditionJsonObject.has(PolicyConstants.IP_RANGE_TYPE.toLowerCase())) {
                JSONObject ipRangeCondition = (JSONObject) conditionJsonObject.get(PolicyConstants.IP_RANGE_TYPE
                        .toLowerCase());
                ThrottleCondition.IPCondition ipCondition = new Gson().fromJson(ipRangeCondition.toString(),
                        ThrottleCondition.IPCondition.class);
                conditionDto.setIpRangeCondition(ipCondition);
            }
            if (conditionJsonObject.has(PolicyConstants.JWT_CLAIMS_TYPE.toLowerCase())) {
                JSONObject jwtClaimConditions = (JSONObject) conditionJsonObject.get(PolicyConstants.JWT_CLAIMS_TYPE
                        .toLowerCase());
                ThrottleCondition.JWTClaimConditions jwtClaimCondition = new Gson().fromJson(jwtClaimConditions
                        .toString(), ThrottleCondition.JWTClaimConditions.class);
                conditionDto.setJwtClaimConditions(jwtClaimCondition);
            }
            if (conditionJsonObject.has(PolicyConstants.HEADER_TYPE.toLowerCase())) {
                JSONObject headerConditionJson = (JSONObject) conditionJsonObject.get(PolicyConstants.HEADER_TYPE
                        .toLowerCase());
                ThrottleCondition.HeaderConditions headerConditions = new Gson().fromJson(headerConditionJson
                        .toString(), ThrottleCondition.HeaderConditions.class);
                conditionDto.setHeaderConditions(headerConditions);
            }

            if (conditionJsonObject.has(PolicyConstants.QUERY_PARAMETER_TYPE.toLowerCase())) {
                JSONObject queryParamConditionJson = (JSONObject) conditionJsonObject.get(PolicyConstants
                        .QUERY_PARAMETER_TYPE.toLowerCase());
                ThrottleCondition.QueryParamConditions queryParamCondition = new Gson().fromJson(queryParamConditionJson
                        .toString(), ThrottleCondition.QueryParamConditions.class);
                conditionDto.setQueryParameterConditions(queryParamCondition);
            }
            conditionDtoList.add(conditionDto);
        }
        conditionDtoList.sort((o1, o2) -> {

            if (o1.getIpCondition() != null && o2.getIpCondition() == null) {
                return -1;
            } else if (o1.getIpCondition() == null && o2.getIpCondition() != null) {
                return 1;
            } else {
                if (o1.getIpRangeCondition() != null && o2.getIpRangeCondition() == null) {
                    return -1;
                } else if (o1.getIpRangeCondition() == null && o2.getIpRangeCondition() != null) {
                    return 1;
                } else {
                    if (o1.getHeaderConditions() != null && o2.getHeaderConditions() == null) {
                        return -1;
                    } else if (o1.getHeaderConditions() == null && o2.getHeaderConditions() != null) {
                        return 1;
                    } else {
                        if (o1.getQueryParameterConditions() != null && o2.getQueryParameterConditions() == null) {
                            return -1;
                        } else if (o1.getQueryParameterConditions() == null && o2.getQueryParameterConditions()
                                != null) {
                            return 1;
                        } else {
                            if (o1.getJwtClaimConditions() != null && o2.getJwtClaimConditions() == null) {
                                return -1;
                            } else if (o1.getJwtClaimConditions() == null && o2.getJwtClaimConditions() != null) {
                                return 1;
                            }
                        }
                    }
                }
            }
            return 0;
        });

        return conditionDtoList;
    }

    /**
     * Parse a JWT and return its claims.
     *
     * @param token JWT token to parse.
     * @return JSONObject containing the claims.
     */
    public static net.minidev.json.JSONObject getJWTClaims(String token) {
        try {
            if (token == null) {
                return null;
            }

            // decoding JWT
            String[] jwtTokenArray = token.split(Pattern.quote("."));
            byte[] jwtByteArray = Base64.decodeBase64(jwtTokenArray[1].getBytes(StandardCharsets.UTF_8));
            String jwtAssertion = new String(jwtByteArray, StandardCharsets.UTF_8);
            net.minidev.json.JSONObject claims = JSONObjectUtils.parse(jwtAssertion);
            return claims;
        } catch (ParseException e) {
            // This exception is supposed to be unreachable.
            // JSONObjectUtils.parse() used above is used within SignedJWT.parse(accessToken)
            // and therefore has already been used in the auth filters.
            log.error("Error while parsing the JWT payload.", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * When sent with a 429 (Too Many Requests) response, this indicates how long to wait before making a new request.
     * {@code Retry-After: <http-date>} format header will be set.
     * <p>
     *     Ex: Retry-After: Fri, 31 Dec 1999 23:59:59 GMT
     * </p>
     *
     * @param context the request context to set the header
     * @param retryTimestamp value of the Retry-After header
     */
    public static void setRetryAfterHeader(RequestContext context, Long retryTimestamp) {
        if (retryTimestamp != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
            dateFormat.setTimeZone(TimeZone.getTimeZone(ThrottleConstants.GMT));
            Date date = new Date(retryTimestamp);
            String retryAfterValue = dateFormat.format(date);
            context.getAddHeaders().put(ThrottleConstants.HEADER_RETRY_AFTER, retryAfterValue);
        }
    }

    public static void setRetryAfterWebsocket(RequestContext requestContext, Long retryTimestamp) {
        if (retryTimestamp != null) {
            Date date = new Date(retryTimestamp);
            long timeInSeconds = date.getTime() / 1000L;
            requestContext.getProperties().put(ThrottleConstants.HEADER_RETRY_AFTER, timeInSeconds);
        }
    }

}
