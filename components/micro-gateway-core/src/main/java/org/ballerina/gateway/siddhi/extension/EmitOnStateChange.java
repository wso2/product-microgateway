/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.ballerina.gateway.siddhi.extension;

import org.ballerinalang.siddhi.annotation.Example;
import org.ballerinalang.siddhi.annotation.Extension;
import org.ballerinalang.siddhi.annotation.Parameter;
import org.ballerinalang.siddhi.annotation.util.DataType;
import org.ballerinalang.siddhi.core.config.SiddhiAppContext;
import org.ballerinalang.siddhi.core.event.ComplexEventChunk;
import org.ballerinalang.siddhi.core.event.stream.StreamEvent;
import org.ballerinalang.siddhi.core.event.stream.StreamEventCloner;
import org.ballerinalang.siddhi.core.event.stream.populater.ComplexEventPopulater;
import org.ballerinalang.siddhi.core.executor.ExpressionExecutor;
import org.ballerinalang.siddhi.core.executor.VariableExpressionExecutor;
import org.ballerinalang.siddhi.core.query.processor.Processor;
import org.ballerinalang.siddhi.core.query.processor.stream.StreamProcessor;
import org.ballerinalang.siddhi.core.util.config.ConfigReader;
import org.ballerinalang.siddhi.query.api.definition.AbstractDefinition;
import org.ballerinalang.siddhi.query.api.definition.Attribute;
import org.ballerinalang.siddhi.query.api.exception.SiddhiAppValidationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a custom extension, written for a certain throttler.
 * Upon arrival of a request, looking at the key in the request, this throttler first decides whether to throttle
 * the request or not.
 * If that decision is different to what it was for the previous request (with the same key),
 * then this processor emits this request as an event; hence the name emitOnStateChange.
 * <p/>
 * If this request is the first request from a certain key, then that requested will be emitted out.
 * <p/>
 * This is useful when the throttler needs to alert only when the throttling decision is changed, in contrast to
 * alerting about every decision taken.
 * <p/>
 * Usage:
 * throttler:emitOnStateChange(key, isThrottled)
 * <p/>
 * Parameters:
 * key: The key coming in the request, based on which throttling decision was made.
 * isThrottled: The throttling decision made.
 * <p/>
 * Example on usage:
 * from DecisionStream#throttler:emitOnStateChange(key, isThrottled)
 * select *
 * insert into AlertStream;
 */

@Extension(name = "emitOnStateChange", namespace = "gateway/throttler", description = "The logger stream processor logs the "
        + "message with or without event for the given log priority.", parameters = {
        @Parameter(name = "throttle.key", description = "The priority/type of this log message.", type = {
                DataType.STRING }),
        @Parameter(name = "is.throttled", description = "This submit the log message.", type = {
                DataType.STRING }) }, examples = {
        @Example(syntax = "from fooStream#log(\"INFO\", \"Sample Event :\", true)\nselect *\ninsert into barStream;",
                description = "This will log as INFO with the message \"Sample Event :\" + fooStream:events."),
        @Example(syntax = "from fooStream#log(\"Sample Event :\", true)\nselect *\ninsert into barStream;",
                description = "This will logs with default log level as INFO."),
        @Example(syntax = "from fooStream#log(\"Sample Event :\", fasle)\nselect *\ninsert into barStream;",
                description = "This will only log message."),
        @Example(syntax = "from fooStream#log(true)\nselect *\ninsert into barStream;",
                description = "This will only log fooStream:events."),
        @Example(syntax = "from fooStream#log(\"Sample Event :\")\nselect *\ninsert into barStream;",
                description = "This will log message and fooStream:events.") })
public class EmitOnStateChange extends StreamProcessor {
    private VariableExpressionExecutor keyExpressionExecutor;
    private VariableExpressionExecutor isThrottledExpressionExecutor;
    private Map<String, Object> throttleStateMap = new HashMap<String, Object>();

    public void start() {
        //Nothing to do.
    }

    public void stop() {
        //Nothing to do.
    }

    public Map<String, Object> currentState() {
        return throttleStateMap;
    }

    public void restoreState(Map<String, Object> map) {
        throttleStateMap = map;
    }

    @Override
    protected void process(ComplexEventChunk<StreamEvent> streamEventChunk, Processor processor,
            StreamEventCloner streamEventCloner, ComplexEventPopulater complexEventPopulater) {
        while (streamEventChunk.hasNext()) {
            StreamEvent event = streamEventChunk.next();
            Boolean currentThrottleState = (Boolean) isThrottledExpressionExecutor.execute(event);
            String key = (String) keyExpressionExecutor.execute(event);
            Boolean lastThrottleState = (Boolean) throttleStateMap.get(key);
            if (currentThrottleState.equals(lastThrottleState) && !currentThrottleState) {
                streamEventChunk.remove();
            } else {
                throttleStateMap.put(key, currentThrottleState);
            }
        }
        nextProcessor.process(streamEventChunk);
    }

    @Override
    protected List<Attribute> init(AbstractDefinition abstractDefinition, ExpressionExecutor[] expressionExecutors,
            ConfigReader configReader, SiddhiAppContext siddhiAppContext) {
        if (attributeExpressionExecutors.length != 2) {
            throw new SiddhiAppValidationException("Invalid no of arguments passed to throttler:emitOnStateChange"
                    + "(key,isThrottled), required 2, but found " + attributeExpressionExecutors.length);
        }
        if (attributeExpressionExecutors[0].getReturnType() != Attribute.Type.STRING) {
            throw new SiddhiAppValidationException("Invalid parameter type found for the argument of "
                    + "throttler:emitOnStateChange(key,isThrottled), " + "required " + Attribute.Type.STRING + ", "
                    + "but found " + attributeExpressionExecutors[0].getReturnType());
        }
        if (attributeExpressionExecutors[1].getReturnType() != Attribute.Type.BOOL) {
            throw new SiddhiAppValidationException("Invalid parameter type found for the argument of "
                    + "throttler:emitOnStateChange(key,isThrottled), " + "required " + Attribute.Type.BOOL
                    + ", but found " + attributeExpressionExecutors[1].getReturnType());
        }
        keyExpressionExecutor = (VariableExpressionExecutor) attributeExpressionExecutors[0];
        isThrottledExpressionExecutor = (VariableExpressionExecutor) attributeExpressionExecutors[1];
        return new ArrayList<Attribute>();
    }
}
