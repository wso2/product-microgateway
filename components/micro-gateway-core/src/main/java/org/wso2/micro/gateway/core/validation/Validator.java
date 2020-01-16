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

package org.wso2.micro.gateway.core.validation;

import org.ballerinalang.jvm.util.exceptions.BLangRuntimeException;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.wso2.micro.gateway.core.utils.ErrorUtils;


/**
 * External function wso2gateway:validator.
 */
public class Validator {

    private static void validator(JSONObject payloadObject, JSONObject jsonSchema) {
        Schema schema = SchemaLoader.load(jsonSchema);
        if (schema == null) {
            return;
        }
        try {
            schema.validate(payloadObject);
        } catch (BLangRuntimeException e) {
            throw ErrorUtils.getBallerinaError("Error occurred when validating the payload", e);
        }

    }

    /**
     * validate request/response payload against given schema.
     *
     * @param jsonSc  schema object
     * @param payload payload to be validate
     */
    public static void validator(String jsonSc, String payload) throws Exception {
        JSONObject payloadObject = new JSONObject(payload);
        JSONObject jsonSchema = new JSONObject(jsonSc);
        try {
            validator(payloadObject, jsonSchema);
        } catch (BLangRuntimeException e) {
            throw ErrorUtils.getBallerinaError("Error occurred when validating the payload", e);
        }
    }
}
