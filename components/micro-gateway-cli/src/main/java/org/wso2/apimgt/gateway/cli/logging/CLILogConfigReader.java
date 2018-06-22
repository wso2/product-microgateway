/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.apimgt.gateway.cli.logging;

import org.wso2.apimgt.gateway.cli.utils.GatewayCmdUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.logging.LogManager;

/**
 * A class for feeding the logging configurations to CLILogManager. This class is set as the config provider through
 * the system property `java.util.logging.config.class`.
 * 
 */
public class CLILogConfigReader {
    private static final PrintStream stderr = System.err;

    public CLILogConfigReader() {
        LogManager logManager = LogManager.getLogManager();
        String initialFile = GatewayCmdUtils.getLoggingPropertiesFileLocation();
        try {
            InputStream configStream = new FileInputStream(initialFile);
            logManager.readConfiguration(configStream);
        } catch (IOException var4) {
            stderr.println("Failed to initialize logging");
        }
    }
}
