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
 */
package org.wso2.choreo.connect.enforcer.jmx;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

/**
 * The ManagementFactory class is a factory class for getting managed beans for
 * the Enforcer.
 */
public class MBeanManagementFactory {

    /*
     * If one already exists, it will return that else it will create a new one and
     * return.
     * 
     * @return A MBeanServer instance.
     */
    public static MBeanServer getMBeanServer() {
        MBeanServer mBeanServer;
        if (MBeanServerFactory.findMBeanServer(null).size() > 0) {
            mBeanServer = MBeanServerFactory.findMBeanServer(null).get(0);
        } else {
            mBeanServer = MBeanServerFactory.createMBeanServer();
        }
        return mBeanServer;
    }
}
