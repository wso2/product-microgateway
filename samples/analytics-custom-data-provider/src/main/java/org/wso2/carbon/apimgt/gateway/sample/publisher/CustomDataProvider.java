/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 *
 */

package org.wso2.carbon.apimgt.gateway.sample.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.common.analytics.collectors.AnalyticsCustomDataProvider;

import java.util.HashMap;
import java.util.Map;

public class CustomDataProvider implements AnalyticsCustomDataProvider {

    private static final Log log = LogFactory.getLog(CustomDataProvider.class);
    private static final String User_AGENT_HEADER = "user-agent";

    public CustomDataProvider() {
        log.info("Successfully initialized");
    }

    @Override public Map<String, Object> getCustomProperties(Object context) {
        Map<String, Object> customProperties = new HashMap<>();
        if (context instanceof HashMap) {
            getPropertiesForCCAnalytics((HashMap<String, Object>) context,customProperties);
            return customProperties;
        }
        return null;
    }

    private void getPropertiesForCCAnalytics(Map<String, Object> ccAnalyticsDataMap, Map<String, Object> customPropertiesMap){
        if(ccAnalyticsDataMap.containsKey(User_AGENT_HEADER)){
            customPropertiesMap.put(User_AGENT_HEADER, ccAnalyticsDataMap.get(User_AGENT_HEADER));
        }
    }
}
