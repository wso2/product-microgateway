// Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/http;

public type AnalyticsRequestFilter object {

    public function __init() {
        if (isELKAnalyticsEnabled || isChoreoAnalyticsEnabled) {
            initDataPublisher4x();
        }
    }

    public function filterRequest(http:Caller caller, http:Request request, http:FilterContext context) returns boolean {
        if (context.attributes.hasKey(SKIP_ALL_FILTERS) && <boolean>context.attributes[SKIP_ALL_FILTERS]) {
            printDebug(KEY_ANALYTICS_FILTER, "Skip all filter annotation set in the service. Skip the filter");
            return true;
        }
        //Filter only if analytics is enabled.
        if (isAnalyticsEnabled || isGrpcAnalyticsEnabled || isELKAnalyticsEnabled || isChoreoAnalyticsEnabled) {
            context.attributes[PROTOCOL_PROPERTY] = caller.protocol;
            error? result = trap setRequestAttributesToContext(request, context);
            if (result is error) {
                printError(KEY_ANALYTICS_FILTER, "Error while setting analytics data in request path", result);
            }
        }
        return true;
    }

    public function filterResponse(http:Response response, http:FilterContext context) returns boolean {
        if (context.attributes.hasKey(SKIP_ALL_FILTERS) && <boolean>context.attributes[SKIP_ALL_FILTERS]) {
            printDebug(KEY_ANALYTICS_FILTER, "Skip all filter annotation set in the service. Skip the filter");
            return true;
        }
        if (isAnalyticsEnabled || isGrpcAnalyticsEnabled) {
            doFilterResponse3x(response, context);
        } else if (isELKAnalyticsEnabled || isChoreoAnalyticsEnabled) {
            doFilterResponse4x(response, context);
        }
        return true;
    }
};
