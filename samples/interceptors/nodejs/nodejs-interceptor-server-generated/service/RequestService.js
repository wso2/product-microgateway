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

'use strict';

const convert = require('xml-js');

/**
 * Handle Request
 *
 * body RequestHandlerRequestBody Content of the request
 (optional)
 * returns RequestHandlerResponseBody
 **/
exports.handleRequest = function (body) {
    const reqBody = Buffer.from(body.requestBody, 'base64').toString('utf8');
    const options = {compact: true, ignoreComment: true, spaces: 4};
    const xmlBody = convert.json2xml(reqBody, options);
    const xmlBodyBase64 = Buffer.from(xmlBody).toString('base64')

    return new Promise(function (resolve, reject) {
        let examples = {};
        examples['application/json'] = {
            "headersToAdd": {
                "x-user": "admin",
            },
            "headersToReplace": {
                "content-type": "application/xml",
            },
            "body": xmlBodyBase64,
        };
        
        if (Object.keys(examples).length > 0) {
            resolve(examples[Object.keys(examples)[0]]);
        } else {
            resolve();
        }
    });
}
