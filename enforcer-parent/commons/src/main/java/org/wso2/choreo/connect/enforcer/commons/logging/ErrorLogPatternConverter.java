/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org).
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

package org.wso2.choreo.connect.enforcer.commons.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.message.ParameterizedMessage;

import java.util.Arrays;

/**
 * This will create a PatternConverter which is used by log4j PatternConverter
 * to format the log event into printable format.
 * %errorDetails can be used in pattern of PatternLayout to get error info to the Log Statements.
 */

@Plugin(name = "ErrorLogPatternConverter", category = "Converter")
@ConverterKeys({"errorDetails"})
public class ErrorLogPatternConverter extends LogEventPatternConverter {
    protected ErrorLogPatternConverter(String name, String style) {
        super(name, style);
    }

    public ErrorLogPatternConverter(String[] options) {
        super("errorDetails", "errorDetails");
    }

    public static ErrorLogPatternConverter newInstance(final String[] options) {
        return new ErrorLogPatternConverter(options);
    }

    @Override
    public void format(LogEvent event, StringBuilder toAppendTo) {
        if ((event.getClass() == Log4jLogEvent.class) && (event.getLevel() == Level.ERROR)) {
            Log4jLogEvent logEvent = (Log4jLogEvent) event;
            if (logEvent.getMessage().getClass() == ParameterizedMessage.class) {
                Object[] parameters = ((ParameterizedMessage) logEvent.getMessage()).getParameters();
                if (Arrays.stream(parameters).anyMatch(p ->
                        p.getClass().getName().equals(ErrorDetails.class.getName()))) {
                    Arrays.stream(parameters)
                        .filter(p -> p.getClass().getName().equals(ErrorDetails.class.getName())).forEach((c) -> {
                            ErrorDetails errorDetails = (ErrorDetails) c;
                            toAppendTo.append(LoggingConstants.LogAttributes.SEVERITY + ":" +
                                    errorDetails.getSeverity() + " " + LoggingConstants.LogAttributes.ERROR_CODE
                                    + ":" + errorDetails.getCode());
                        });
                } else {
                    toAppendTo.append(LoggingConstants.LogAttributes.SEVERITY + ":" +
                            LoggingConstants.Severity.DEFAULT + " " +
                            LoggingConstants.LogAttributes.ERROR_CODE + ":" + 0);
                }
            } else {
                toAppendTo.append(LoggingConstants.LogAttributes.SEVERITY + ":" +
                        LoggingConstants.Severity.DEFAULT + " " +
                        LoggingConstants.LogAttributes.ERROR_CODE + ":" + 0);
            }
        }
    }
}
