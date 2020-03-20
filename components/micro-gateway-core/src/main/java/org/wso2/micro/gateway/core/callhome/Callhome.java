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

package org.wso2.micro.gateway.core.callhome;

import org.wso2.callhome.CallHomeExecutor;
import org.wso2.callhome.utils.CallHomeInfo;
import org.wso2.callhome.utils.Util;
import org.wso2.micro.gateway.core.Constants;

/**
 * Invoke call home.
 */
public class Callhome {

    /**
     * run call home.
     */
    public static void runCallHome(String trustStoreLocation, String trustStorePassword) {
        String productHome = getRuntimeHome();

        CallHomeInfo callhomeinfo = Util.createCallHomeInfo(productHome, trustStoreLocation, trustStorePassword);
        CallHomeExecutor.execute(callhomeinfo);
    }

    /**
     * Get runtime home location.
     *
     * @return runtime home location
     */
    private static String getRuntimeHome() {
        return System.getProperty(Constants.RUNTIME_HOME_PATH);
    }

    /**
     * Get truststore location.
     *
     * @return truststore location
     */
    public static String getTrustStoreLocation(String fullpath) {
        String homePathConst = "\\$\\{mgw-runtime.home}";
        String homePath = System.getProperty(Constants.RUNTIME_HOME_PATH);
        String correctPath = fullpath.replaceAll(homePathConst, homePath);
        return correctPath;
    }
}
