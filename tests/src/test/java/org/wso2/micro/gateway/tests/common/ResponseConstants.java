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
    public static final String petByIdResponse = "{\"id\": 9199424981609281000," + "  \"category\": {"
            + "    \"id\": 0," + "    \"name\": \"string\"" + "  }," + "  \"name\": \"doggie\","
            + "  \"photoUrls\": [" + "    \"string\"" + "  ]," + "  \"tags\": [" + "    {"
            + "      \"id\": 0," + "      \"name\": \"string\"" + "    }" + "  ],"
            + "  \"status\": \"available\"" + "}";
    public static final String petByIdResponseV1 = "{\"id\": 33333333333333," + "  \"category\": {"
            + "    \"id\": 0," + "    \"name\": \"string\"" + "  }," + "  \"name\": \"doggieNew\","
            + "  \"photoUrls\": [" + "    \"string\"" + "  ]," + "  \"tags\": [" + "    {"
            + "      \"id\": 0," + "      \"name\": \"string\"" + "    }" + "  ],"
            + "  \"status\": \"available\"" + "}";
}
