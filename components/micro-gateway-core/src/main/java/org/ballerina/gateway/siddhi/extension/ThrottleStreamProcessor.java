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
import org.ballerinalang.siddhi.core.event.ComplexEvent;
import org.ballerinalang.siddhi.core.event.ComplexEventChunk;
import org.ballerinalang.siddhi.core.event.state.StateEvent;
import org.ballerinalang.siddhi.core.event.stream.StreamEvent;
import org.ballerinalang.siddhi.core.event.stream.StreamEventCloner;
import org.ballerinalang.siddhi.core.event.stream.populater.ComplexEventPopulater;
import org.ballerinalang.siddhi.core.executor.ConstantExpressionExecutor;
import org.ballerinalang.siddhi.core.executor.ExpressionExecutor;
import org.ballerinalang.siddhi.core.executor.VariableExpressionExecutor;
import org.ballerinalang.siddhi.core.query.processor.Processor;
import org.ballerinalang.siddhi.core.query.processor.SchedulingProcessor;
import org.ballerinalang.siddhi.core.query.processor.stream.StreamProcessor;
import org.ballerinalang.siddhi.core.query.processor.stream.window.FindableProcessor;
import org.ballerinalang.siddhi.core.table.Table;
import org.ballerinalang.siddhi.core.util.Scheduler;
import org.ballerinalang.siddhi.core.util.collection.operator.CompiledCondition;
import org.ballerinalang.siddhi.core.util.collection.operator.MatchingMetaInfoHolder;
import org.ballerinalang.siddhi.core.util.collection.operator.Operator;
import org.ballerinalang.siddhi.core.util.config.ConfigReader;
import org.ballerinalang.siddhi.core.util.parser.OperatorParser;
import org.ballerinalang.siddhi.query.api.definition.AbstractDefinition;
import org.ballerinalang.siddhi.query.api.definition.Attribute;
import org.ballerinalang.siddhi.query.api.exception.SiddhiAppValidationException;
import org.ballerinalang.siddhi.query.api.expression.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Throttle stream processor class
 */
@Extension(name = "timeBatch", namespace = "gateway/throttler", description =
        "A batch (tumbling) time window that holds events that arrive during window.time periods, "
                + "and gets updated for each window.time.", parameters = {
        @Parameter(name = "window.time", description = "The batch time period for which the window should hold events.",
                type = { DataType.INT, DataType.LONG, DataType.TIME }),
        @Parameter(name = "start.time", description = "This specifies an offset in milliseconds in order to start the "
                + "window at a time different to the standard time.", type = { DataType.INT }) }, examples = {
        @Example(syntax = "define window cseEventWindow (symbol string, price float, volume int) "
                + "timeBatch(20) output all events;\n" + "@info(name = 'query0')\n" + "from cseEventStream\n"
                + "insert into cseEventWindow;\n" + "@info(name = 'query1')\n" + "from cseEventWindow\n"
                + "select symbol, sum(price) as price\n" + "insert all events into outputStream ;", description =
                "This will processing events arrived every 20 milliseconds" + " as a batch and out put all events.") })
public class ThrottleStreamProcessor extends StreamProcessor implements SchedulingProcessor, FindableProcessor {
    private static final Logger log = LoggerFactory.getLogger(ThrottleStreamProcessor.class);
    private static final String EXPIRY_TIME_STAMP = "expiryTimeStamp";
    private long timeInMilliSeconds;
    private ComplexEventChunk<StreamEvent> expiredEventChunk = new ComplexEventChunk<StreamEvent>(true);
    private Scheduler scheduler;
    private SiddhiAppContext siddhiAppContext;
    private long expireEventTime = -1;
    private long startTime = -1;

    public ThrottleStreamProcessor() {
        scheduler = null;
    }

    public synchronized void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public synchronized Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    protected List<Attribute> init(AbstractDefinition abstractDefinition, ExpressionExecutor[] expressionExecutors,
            ConfigReader configReader, SiddhiAppContext siddhiAppContext) {
        this.siddhiAppContext = siddhiAppContext;

        if (attributeExpressionExecutors.length == 1) {
            if (attributeExpressionExecutors[0] instanceof ConstantExpressionExecutor) {
                if (attributeExpressionExecutors[0].getReturnType() == Attribute.Type.INT) {
                    timeInMilliSeconds = (Integer) ((ConstantExpressionExecutor) attributeExpressionExecutors[0])
                            .getValue();

                } else if (attributeExpressionExecutors[0].getReturnType() == Attribute.Type.LONG) {
                    timeInMilliSeconds = (Long) ((ConstantExpressionExecutor) attributeExpressionExecutors[0])
                            .getValue();
                } else {
                    throw new SiddhiAppValidationException("Throttle batch window's 1st parameter attribute should be "
                            + "either int or long, but found " + attributeExpressionExecutors[0].getReturnType());
                }
            } else {
                throw new SiddhiAppValidationException("Throttle batch window 1st parameter needs to be constant "
                        + "parameter attribute but found a dynamic attribute " + attributeExpressionExecutors[0]
                        .getClass().getCanonicalName());
            }
        } else if (attributeExpressionExecutors.length == 2) {
            if (attributeExpressionExecutors[0] instanceof ConstantExpressionExecutor) {
                if (attributeExpressionExecutors[0].getReturnType() == Attribute.Type.INT) {
                    timeInMilliSeconds = (Integer) ((ConstantExpressionExecutor) attributeExpressionExecutors[0])
                            .getValue();

                } else if (attributeExpressionExecutors[0].getReturnType() == Attribute.Type.LONG) {
                    timeInMilliSeconds = (Long) ((ConstantExpressionExecutor) attributeExpressionExecutors[0])
                            .getValue();
                } else {
                    throw new SiddhiAppValidationException("Throttle batch window's 1st parameter attribute should be "
                            + "either int or long, but found " + attributeExpressionExecutors[0].getReturnType());
                }
            } else {
                throw new SiddhiAppValidationException("Throttle batch window 1st parameter needs to be constant "
                        + "attribute but found a dynamic attribute " + attributeExpressionExecutors[0].getClass()
                        .getCanonicalName());
            }

            if (attributeExpressionExecutors[1].getReturnType() == Attribute.Type.INT) {
                startTime = Integer.parseInt(
                        String.valueOf(((ConstantExpressionExecutor) attributeExpressionExecutors[1]).getValue()));
            } else if (attributeExpressionExecutors[1].getReturnType() == Attribute.Type.LONG) {
                startTime = Long.parseLong(
                        String.valueOf(((ConstantExpressionExecutor) attributeExpressionExecutors[1]).getValue()));
            } else {
                throw new SiddhiAppValidationException(
                        "Throttle batch window 2nd parameter needs to be a Long " + "or Int type but found a "
                                + attributeExpressionExecutors[2].getReturnType());
            }
        } else {
            throw new SiddhiAppValidationException("Throttle batch window should only have one/two parameter "
                    + "(<int|long|time> windowTime (and <int|long> startTime), but found "
                    + attributeExpressionExecutors.length + " input attributes");
        }

        List<Attribute> attributeList = new ArrayList<Attribute>();
        attributeList.add(new Attribute(EXPIRY_TIME_STAMP, Attribute.Type.LONG));
        return attributeList;
    }

    @Override
    protected void process(ComplexEventChunk<StreamEvent> complexEventChunk, Processor processor,
            StreamEventCloner streamEventCloner, ComplexEventPopulater complexEventPopulater) {
        synchronized (this) {
            if (expireEventTime == -1) {
                long currentTime = siddhiAppContext.getTimestampGenerator().currentTime();
                if (startTime != -1) {
                    expireEventTime = addTimeShift(currentTime);
                } else {
                    expireEventTime = siddhiAppContext.getTimestampGenerator().currentTime() + timeInMilliSeconds;
                }
                if (scheduler != null) {
                    scheduler.notifyAt(expireEventTime);
                } else {
                    log.error("scheduler is not initiated");
                }
            }
            long currentTime = siddhiAppContext.getTimestampGenerator().currentTime();
            boolean sendEvents;
            if (currentTime >= expireEventTime) {
                expireEventTime += timeInMilliSeconds;
                if (scheduler != null) {
                    scheduler.notifyAt(expireEventTime);
                } else {
                    log.error("scheduler is not initiated");
                }
                sendEvents = true;
            } else {
                sendEvents = false;
            }

            while (complexEventChunk.hasNext()) {
                StreamEvent streamEvent = complexEventChunk.next();
                if (streamEvent.getType() != ComplexEvent.Type.CURRENT) {
                    continue;
                }

                complexEventPopulater.populateComplexEvent(streamEvent, new Object[] { expireEventTime });
                StreamEvent clonedStreamEvent = streamEventCloner.copyStreamEvent(streamEvent);
                clonedStreamEvent.setType(StreamEvent.Type.EXPIRED);
                clonedStreamEvent.setTimestamp(expireEventTime);
                expiredEventChunk.add(clonedStreamEvent);
            }
            if (sendEvents) {
                expiredEventChunk.reset();
                if (expiredEventChunk.getFirst() != null) {
                    complexEventChunk.add(expiredEventChunk.getFirst());
                }
                expiredEventChunk.clear();
            }
        }
        if (complexEventChunk.getFirst() != null) {
            complexEventChunk.setBatch(true);
            nextProcessor.process(complexEventChunk);
            complexEventChunk.setBatch(false);
        }
    }

    public void start() {
        //Do nothing
    }

    public void stop() {
        //Do nothing
    }

    public Map<String, Object> currentState() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("0", expiredEventChunk);
        return map;
    }

    public void restoreState(Map<String, Object> map) {
        //Do nothing
    }

    public synchronized StreamEvent find(StateEvent stateEvent, CompiledCondition compiledCondition) {
        if (compiledCondition instanceof Operator) {
            return ((Operator) compiledCondition).find(stateEvent, this.expiredEventChunk, this.streamEventCloner);
        } else {
            return null;
        }
    }

    private long addTimeShift(long currentTime) {
        long timePassedUntilNow = (currentTime - startTime) % timeInMilliSeconds;
        return currentTime + (timeInMilliSeconds - timePassedUntilNow);
    }

    public CompiledCondition compileCondition(Expression expression, MatchingMetaInfoHolder matchingMetaInfoHolder,
            SiddhiAppContext siddhiAppContext, List<VariableExpressionExecutor> list, Map<String, Table> map,
            String s) {
        return OperatorParser
                .constructOperator(this.expiredEventChunk, expression, matchingMetaInfoHolder, siddhiAppContext, list,
                        map, this.queryName);
    }
}
