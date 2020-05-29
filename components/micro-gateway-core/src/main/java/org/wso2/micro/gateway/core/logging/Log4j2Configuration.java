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

    private static final String LOG4J_CONFIGURATION_FILE = "log4j.configurationFile";
    private static final String LOG4J_CONFIGURATION_NAME = "nativeImplLog4jConfig";
    private static final String APPENDER_NAME = "Stdout";
    private static final String APPENDER_PLUGIN_TYPE = "Console";
    private static final String APPENDER_LAYOUT_PLUGIN_TYPE = "PatternLayout";
    private static final String APPENDER_PATTERN_KEY = "pattern";
    private static final String APPENDER_PATTERN_VALUE = "%d %-5p [%c] - [%c{1}] %x %m%n";

    private static final String TRACE_LOG_LEVEL = "TRACE";
    private static final String DEBUG_LOG_LEVEL = "DEBUG";
    private static final String INFO_LOG_LEVEL = "INFO";
    private static final String WARN_LOG_LEVEL = "WARN";

    public static void initialize(String logLevel) {
        if (System.getProperty(LOG4J_CONFIGURATION_FILE) == null) {
            ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder()
                    .setStatusLevel(Level.ERROR)
                    .setConfigurationName(LOG4J_CONFIGURATION_NAME);
            AppenderComponentBuilder appenderBuilder = builder.newAppender(APPENDER_NAME, APPENDER_PLUGIN_TYPE)
                    .add(builder.newLayout(APPENDER_LAYOUT_PLUGIN_TYPE)
                            .addAttribute(APPENDER_PATTERN_KEY, APPENDER_PATTERN_VALUE));
            builder.add(appenderBuilder);
            switch (logLevel) {
                case TRACE_LOG_LEVEL:
                    builder.add(builder.newRootLogger(Level.TRACE).add(builder.newAppenderRef(APPENDER_NAME)));
                    break;
                case DEBUG_LOG_LEVEL:
                    builder.add(builder.newRootLogger(Level.DEBUG).add(builder.newAppenderRef(APPENDER_NAME)));
                    break;
                case INFO_LOG_LEVEL:
                    builder.add(builder.newRootLogger(Level.INFO).add(builder.newAppenderRef(APPENDER_NAME)));
                    break;
                case WARN_LOG_LEVEL:
                    builder.add(builder.newRootLogger(Level.WARN).add(builder.newAppenderRef(APPENDER_NAME)));
                    break;
                default:
                    builder.add(builder.newRootLogger(Level.ERROR).add(builder.newAppenderRef(APPENDER_NAME)));
                    break;
            }
            Configurator.initialize(builder.build());
        }
    }
}
