/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.micro.gateway.tests.common;

public class ResponseConstants {

    public static final String responseBody = "{\"id\":111111148, \"category\":{ \"id\":0, \"name\":\"ABCD\" }," +
            " \"name\":\"doggieUpdated\", \"photoUrls\":[ \"SampleImage1.png\" ], \"tags\":[ { \"id\":0, " +
            "\"name\":\"TestTag1\" } ], \"status\":\"pending\"}";
    public static final String responseBodyV1 = "{\"id\":222222222, \"category\":{ \"id\":1, \"name\":\"Test\" }," +
            " \"name\":\"doggie\", \"photoUrls\":[ \"SampleImage1.png\" ], \"tags\":[ { \"id\":0, " +
            "\"name\":\"TestTag1\" } ], \"status\":\"pending\"}";
    public static final String petByIdResponse = "{\"id\":2, \"category\":{\"id\":1, \"name\":\"John Doe\"}, " +
            "\"name\":\"shre\", \"photoUrls\":[\"ArrayItem1\"], \"tags\":[{\"id\":1, \"name\":\"TfNSW\"}]," +
            " \"status\":\"hello\"}";
    public static final String petByIdResponseV1 = "{\"id\":2, \"category\":{\"id\":1, \"name\":\"John Doe\"}, " +
            "\"name\":\"shre\", \"photoUrls\":[\"ArrayItem1\"], \"tags\":[{\"id\":1, \"name\":\"TfNSW\"}], " +
            "\"status\":\"hello\"}";
    public static final String getPetResponse = "{\"id\":2, \"category\":{\"id\":1, \"name\":\"John Doe\"}," +
            " \"name\":\"shre\", \"photoUrls\":[\"ArrayItem1\"], \"tags\":[{\"id\":1, \"name\":\"TfNSW\"}]," +
            " \"status\":\"hello\"}";
    public static final String storeInventoryResponse = "{ \"AVAILABLE\": 1," + "  \"string\": 2,"
            + "  \"pending\": 2," + "  \"available\": 233539," + "  \"PENDING\": 1" + "}";
    public static final String RESPONSE_VALID_JWT_TRANSFORMER = "{\"id\":3, \"category\":{\"id\":1, \"name\":\"John Doe\"}, " +
            "\"name\":\"shre\", \"photoUrls\":[\"ArrayItem1\"], \"tags\":[{\"id\":1, \"name\":\"TfNSW\"}], " +
            "\"status\":\"hello\"}";
    public static final String PER_RESOURCE_THROTTLING_RESPONSE = "{\"fault\":{\"code\":900802," +
            " \"message\":\"Message throttled out\", \"description\":\"You have exceeded your quota\"}}";
    public static final String PER_API_THROTTLING_RESPONSE = "{\"fault\":{\"code\":900800," +
            " \"message\":\"Message throttled out\", \"description\":\"You have exceeded your quota\"}}";
    public static final String NONEXISTING_THROTTLEPOLICY_RESPONSE = "{\"fault\":{\"code\":900809, " +
            "\"message\":\"Internal server error occured\", \"description\":\"POLICY ENFORCEMENT ERROR\"}}";
    public static final String AUTHENTICATION_FAILURE_RESPONSE = "{\"fault\":\"Authorization credentials are not " +
            "provided.\"}";
    public static final String PER_RESOURCE_REQUEST_INTERCEPTOR_RESPONSE = "{\"Intercept\":{\"RequestCode\":" +
            "\"e123\", \"message\":\"Successfully intercepted\", \"description\":\"Description\"}}";
    public static final String RESPONSE_INTERCEPTOR_RESPONSE_HEDAER = "ResponseHeader";
    public static final String API_REQUEST_INTERCEPTOR_RESPONSE = "{\"APIIntercept\":{\"RequestCode\":\"e123\", " +
            "\"message\":\"Successfully intercepted\", \"description\":\"Description\"}}";
    public static final String PER_APIRESPONSE_HEADER = "PerAPIResponse_Header";
    public static final String PAYLOAD = "payload";
    public static final String JSON_RESPONSE = ":application/json:Accept:Cache-Control:Connection:Content-Length"
            + ":content-type:Host:Pragma::/petstore/v1/user?test=value1&test2=value2:POST:1.1:"
            + "{test2=value2, test=value1}:value1:{\"hello\":\"world\"}";
    public static final String JSON_ARRAY_RESPONSE = ":application/json:Accept:Cache-Control:Connection:Content-Length"
            + ":content-type:Host:Pragma:X_JWT::/petstore/v1/user?test=value1&test2=value2:POST:1.1:{test2=value2, "
            + "test=value1}:value1:[{\"name\":\"foo\",\"age\":\"20\"},{\"name\":\"bar\",\"age\":\"30\"}]";
    public static final String XML_RESPONSE = ":text/xml:Accept:Cache-Control:Connection:Content-Length"
            + ":content-type:Host:Pragma::/petstore/v1/user?test=value1&test2=value2:POST:1.1:"
            + "{test2=value2, test=value1}:value1:<test><msg>hello</msg></test>";
    public static final String VALIDATION_RESPONSE = "{\"fault\":{\"code\":400, \"message\":\"Bad Request\", " +
            "\"description\":\"#: required key [name] not found, #: required key [photoUrls] not found, \"}}";
    public static final String  INVALID_RESPONSE = "{\"fault\":{\"code\":500, \"message\":\"Bad Response\", " +
            "\"description\":\"#/status: hello is not a valid enum value, \"}}";

    public static final String INTERCEPT_JSON_RESPONSE = "\"{\"city\":\"chicago\",\"name\":\"jon doe\",\"age\":\"22\"}\"";
}
