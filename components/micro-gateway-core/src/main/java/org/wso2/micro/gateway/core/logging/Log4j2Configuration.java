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

package org.wso2.micro.gateway.core.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

/**
 * To configure the log4j2 properties for native implementation.
 */
public class Log4j2Configuration {

    private static final String log4jConfigurationProperty = "log4j.configurationFile";
    private static final String log4jConfigurationName = "nativeImplLog4jConfig";
    private static final String appenderName = "Stdout";
    private static final String appenderPluginType = "Console";
    private static final String appenderLayoutPluginType = "PatternLayout";
    private static final String appenderPatternKey = "pattern";
    private static final String appenderPatternValue = "%d %-5p [%c] - [%c{1}] %x %m%n";

    private static final String traceLogLevel = "TRACE";
    private static final String debugLogLevel = "DEBUG";
    private static final String infoLogLevel = "INFO";
    private static final String warnLogLevel = "WARN";

    public static void initialize(String logLevel) {
        if (System.getProperty(log4jConfigurationProperty) == null) {
            ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder()
                    .setStatusLevel(Level.ERROR)
                    .setConfigurationName(log4jConfigurationName);
            AppenderComponentBuilder appenderBuilder = builder.newAppender(appenderName, appenderPluginType)
                    .add(builder.newLayout(appenderLayoutPluginType)
                            .addAttribute(appenderPatternKey, appenderPatternValue));
            builder.add(appenderBuilder);
            switch (logLevel) {
                case traceLogLevel:
                    builder.add(builder.newRootLogger(Level.TRACE).add(builder.newAppenderRef(appenderName)));
                    break;
                case debugLogLevel:
                    builder.add(builder.newRootLogger(Level.DEBUG).add(builder.newAppenderRef(appenderName)));
                    break;
                case infoLogLevel:
                    builder.add(builder.newRootLogger(Level.INFO).add(builder.newAppenderRef(appenderName)));
                    break;
                case warnLogLevel:
                    builder.add(builder.newRootLogger(Level.WARN).add(builder.newAppenderRef(appenderName)));
                    break;
                default:
                    builder.add(builder.newRootLogger(Level.ERROR).add(builder.newAppenderRef(appenderName)));
                    break;
            }
            Configurator.initialize(builder.build());
        }
    }
}
