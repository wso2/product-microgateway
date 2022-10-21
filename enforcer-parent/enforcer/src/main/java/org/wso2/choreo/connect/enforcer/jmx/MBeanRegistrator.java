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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.commons.logging.ErrorDetails;
import org.wso2.choreo.connect.enforcer.commons.logging.LoggingConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

/**
 * The class which is responsible for registering MBeans.
 */
public class MBeanRegistrator {
    private static final Logger logger = LogManager.getLogger(MBeanRegistrator.class);
    private static List<ObjectName> mBeans = new ArrayList<>();

    private static final String SERVER_PACKAGE = "org.wso2.choreo.connect.enforcer";

    private MBeanRegistrator() {
    }

    /**
     * Registers an object as an MBean with the MBean server.
     *
     * @param mBeanInstance - The MBean to be registered as an MBean.
     */
    public static void registerMBean(Object mBeanInstance) throws RuntimeException {

        if (JMXUtils.isJMXMetricsEnabled()) {
            String className = mBeanInstance.getClass().getName();
            if (className.indexOf('.') != -1) {
                className = className.substring(className.lastIndexOf('.') + 1);
            }

            String objectName = String.format("%s:type=%s", SERVER_PACKAGE, className);
            try {
                MBeanServer mBeanServer = MBeanManagementFactory.getMBeanServer();
                Set set = mBeanServer.queryNames(new ObjectName(objectName), null);
                if (set.isEmpty()) {
                    try {
                        ObjectName name = new ObjectName(objectName);
                        mBeanServer.registerMBean(mBeanInstance, name);
                        mBeans.add(name);
                    } catch (InstanceAlreadyExistsException e) {
                        String msg = "MBean " + objectName + " already exists";
                        logger.error(msg, ErrorDetails.errorLog(LoggingConstants.Severity.MINOR, 6801), e);
                        throw new RuntimeException(msg, e);
                    } catch (MBeanRegistrationException | NotCompliantMBeanException e) {
                        String msg = "Execption when registering MBean";
                        logger.error(msg, ErrorDetails.errorLog(LoggingConstants.Severity.MINOR, 6802), e);
                        throw new RuntimeException(msg, e);
                    }
                } else {
                    String msg = "MBean " + objectName + " already exists";
                    logger.error(msg, ErrorDetails.errorLog(LoggingConstants.Severity.MINOR, 6803));
                    throw new RuntimeException(msg);
                }
            } catch (MalformedObjectNameException e) {
                String msg = "Could not register " + mBeanInstance.getClass() + " MBean";
                logger.error(msg, ErrorDetails.errorLog(LoggingConstants.Severity.MINOR, 6804), e);
                throw new RuntimeException(msg, e);
            }
        } else {
            logger.debug("JMX Metrics should be enabled to register MBean instance: {}",
                    mBeanInstance.getClass().getName());
        }
    }
}
