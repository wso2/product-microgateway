/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.mockbackend;


/**
 * Response messages for the mock backend server.
 */
public class ResponseConstants {

    public static final String RESPONSE_BODY = "{\"id\":111111148, \"category\":{ \"id\":0, \"name\":\"ABCD\" }," +
            " \"name\":\"doggieUpdated\", \"photoUrls\":[ \"SampleImage1.png\" ], \"tags\":[ { \"id\":0, " +
            "\"name\":\"TestTag1\" } ], \"status\":\"pending\"}";
    public static final String PET_BY_ID_RESPONSE = "{\"id\":2, \"category\":{\"id\":1, \"name\":\"John Doe\"}, " +
            "\"name\":\"shre\", \"photoUrls\":[\"ArrayItem1\"], \"tags\":[{\"id\":1, \"name\":\"TfNSW\"}]," +
            " \"status\":\"hello\"}";
    public static final String GET_PET_RESPONSE = "{\"id\":2, \"category\":{\"id\":1, \"name\":\"John Doe\"}," +
            " \"name\":\"shre\", \"photoUrls\":[\"ArrayItem1\"], \"tags\":[{\"id\":1, \"name\":\"TfNSW\"}]," +
            " \"status\":\"hello\"}";
    public static final String STORE_INVENTORY_RESPONSE = "{ \"AVAILABLE\": 1," + "  \"string\": 2,"
            + "  \"pending\": 2," + "  \"available\": 233539," + "  \"PENDING\": 1" + "}";
    public static final String RESPONSE_VALID_JWT_TRANSFORMER = "{\"id\":3, \"category\":{\"id\":1, \"name\":\"John Doe\"}, " +
            "\"name\":\"shre\", \"photoUrls\":[\"ArrayItem1\"], \"tags\":[{\"id\":1, \"name\":\"TfNSW\"}], " +
            "\"status\":\"hello\"}";
    public static final String API_SANDBOX_RESPONSE = "{\"id\":111111155, \"category\":{ \"id\":0, \"name\":\"ABCD\" }," +
            " \"name\":\"doggieUpdated\", \"photoUrls\":[ \"SampleImage1.png\" ], \"tags\":[ { \"id\":0, " +
            "\"name\":\"TestTag1\" } ], \"status\":\"pending\"}";


    public static final String AUTHENTICATION_FAILURE_RESPONSE = "{\"fault\":\"Authorization credentials are not " +
            "provided.\"}";
    public static final String AUTHZ_FAILURE_RESPONSE = "{\"fault\":\"Forbidden.\"}";


    public static final String userResponse = "{ \"name\": \"john\" }";

    public static final String INVALID_JWT_RESPONSE ="\"{\"header\":\"not available\"}\"";
    public static final String VALID_JWT_RESPONSE ="\"{\"header\":\"available\"}\"";

    public static final String VALID_REMOVE_HEADER_RESPONSE ="\"{\"header\":\"not available\"}\"";
    public static final String INVALID_REMOVE_HEADER_RESPONSE ="\"{\"header\":\"available\"}\"";

}
