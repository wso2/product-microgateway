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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

import java.net.URI;

/**
 * To configure the log4j2 properties for native implementation programmatically.
 */
public class MgwLog4j2ConfigurationFactory extends ConfigurationFactory {

    private static final String LOG4J_CONFIGURATION_FILE = "log4j.configurationFile";
    private static final String APPENDER_NAME = "Stdout";
    private static final String APPENDER_PLUGIN_TYPE = "Console";
    private static final String APPENDER_LAYOUT_PLUGIN_TYPE = "PatternLayout";
    private static final String APPENDER_PATTERN_KEY = "pattern";
    private static final String APPENDER_PATTERN_VALUE = "%d %-5p [%c] - [%c{1}] %x %m%n";

    private static final String TRACE_LOG_LEVEL = "TRACE";
    private static final String DEBUG_LOG_LEVEL = "DEBUG";
    private static final String WARN_LOG_LEVEL = "WARN";
    private static final String ERROR_LOG_LEVEL = "ERROR";

    static Configuration createConfiguration(final String name, ConfigurationBuilder<BuiltConfiguration> builder) {
        builder.setConfigurationName(name);
        builder.setStatusLevel(Level.ERROR);
        AppenderComponentBuilder appenderBuilder = builder.newAppender(APPENDER_NAME, APPENDER_PLUGIN_TYPE);
        appenderBuilder.add(builder.newLayout(APPENDER_LAYOUT_PLUGIN_TYPE).
                addAttribute(APPENDER_PATTERN_KEY, APPENDER_PATTERN_VALUE));
        builder.add(appenderBuilder);
        builder.add(builder.newRootLogger(Level.ERROR).add(builder.newAppenderRef(APPENDER_NAME)));
        return builder.build();
    }

    @Override
    public Configuration getConfiguration(final LoggerContext loggerContext, final ConfigurationSource source) {
        return getConfiguration(loggerContext, source.toString(), null);
    }

    @Override
    public Configuration getConfiguration(final LoggerContext loggerContext, final String name,
                                          final URI configLocation) {
        ConfigurationBuilder<BuiltConfiguration> builder = newConfigurationBuilder();
        return createConfiguration(name, builder);
    }

    @Override
    protected String[] getSupportedTypes() {
        return new String[]{"*"};
    }

    /**
     * Modify the default programmatic configuration for log4j2 based on the log level set in gateway.
     * @param logLevel log level
     */
    public static void modifyAfterInitialization(String logLevel) {
        //if log4j2 configuration file is provided, the modification should not happen
        if (System.getProperty(LOG4J_CONFIGURATION_FILE) != null) {
            return;
        }
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Level log4j2LogLevel = Level.INFO;
        switch (logLevel) {
            case TRACE_LOG_LEVEL:
                log4j2LogLevel = Level.TRACE;
                break;
            case DEBUG_LOG_LEVEL:
                log4j2LogLevel = Level.DEBUG;
                break;
            case WARN_LOG_LEVEL:
                log4j2LogLevel = Level.WARN;
                break;
            case ERROR_LOG_LEVEL:
                log4j2LogLevel = Level.ERROR;
                break;
        }
        ctx.getConfiguration().getRootLogger().setLevel(log4j2LogLevel);
        ctx.updateLoggers();
    }
}
