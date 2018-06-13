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

import org.ballerinalang.siddhi.core.SiddhiAppRuntime;
import org.ballerinalang.siddhi.core.SiddhiManager;
import org.ballerinalang.siddhi.core.event.Event;
import org.ballerinalang.siddhi.core.query.output.callback.QueryCallback;
import org.ballerinalang.siddhi.core.stream.input.InputHandler;
import org.ballerinalang.siddhi.core.util.EventPrinter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ThrottleTimeBatchWindowTestCase {
    private int inEventCount;
    private int removeEventCount;
    private boolean eventArrived;
    private Event lastRemoveEvent;
    private Event lastCurrentEvent;

    @Before
    public void init() {
        inEventCount = 0;
        removeEventCount = 0;
        eventArrived = false;
    }

    @Test
    public void throttleTimeWindowBatchTest1() throws InterruptedException {
        SiddhiManager siddhiManager = new SiddhiManager();
        String cseEventStream = "" + "define stream cseEventStream (symbol string, price float, volume int);";
        String query = "" + "@info(name = 'query1') " + "from cseEventStream#throttler:timeBatch(5 sec) "
                + "select symbol,sum(price) as sumPrice,volume, expiryTimeStamp "
                + "insert all events into outputStream ;";

        SiddhiAppRuntime executionPlanRuntime = siddhiManager.createSiddhiAppRuntime(cseEventStream + query);

        executionPlanRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timeStamp, inEvents, removeEvents);
                if (inEvents != null) {
                    inEventCount = inEventCount + inEvents.length;
                } else if (removeEvents != null) {
                    removeEventCount = removeEventCount + removeEvents.length;
                }
                eventArrived = true;
            }

        });

        InputHandler inputHandler = executionPlanRuntime.getInputHandler("cseEventStream");
        executionPlanRuntime.start();
        inputHandler.send(new Object[] { "IBM", 700f, 0 });
        Thread.sleep(500);
        inputHandler.send(new Object[] { "WSO2", 60.5f, 1 });
        Thread.sleep(6000);
        inputHandler.send(new Object[] { "IBM", 700f, 0 });
        inputHandler.send(new Object[] { "WSO2", 60.5f, 1 });
        Thread.sleep(6000);
        Assert.assertEquals(4, inEventCount);
        Assert.assertEquals(2, removeEventCount);
        Assert.assertTrue(eventArrived);
        executionPlanRuntime.shutdown();
    }

    @Test
    public void throttleTimeWindowBatchTest2() throws InterruptedException {
        SiddhiManager siddhiManager = new SiddhiManager();

        String cseEventStream = "" + "define stream cseEventStream (symbol string, price float, volume int);";
        String query = "" + "@info(name = 'query1') " + "from cseEventStream#throttler:timeBatch(5 sec , 0) "
                + "select symbol,sum(price) as sumPrice,volume, expiryTimeStamp "
                + "insert all events into outputStream ;";

        SiddhiAppRuntime executionPlanRuntime = siddhiManager.createSiddhiAppRuntime(cseEventStream + query);

        executionPlanRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timeStamp, inEvents, removeEvents);
                if (inEvents != null) {
                    inEventCount = inEventCount + inEvents.length;
                } else if (removeEvents != null) {
                    removeEventCount = removeEventCount + removeEvents.length;
                    lastRemoveEvent = removeEvents[removeEvents.length - 1];
                }
                eventArrived = true;
            }

        });

        InputHandler inputHandler = executionPlanRuntime.getInputHandler("cseEventStream");
        executionPlanRuntime.start();
        inputHandler.send(new Object[] { "IBM", 700f, 0 });
        inputHandler.send(new Object[] { "WSO2", 60.5f, 1 });
        Thread.sleep(10000);
        Assert.assertEquals(2, inEventCount);
        Assert.assertEquals("WSO2", lastRemoveEvent.getData()[0]);
        Assert.assertTrue(eventArrived);
        executionPlanRuntime.shutdown();
    }

    @Ignore
    public void throttleTimeWindowBatchTest3() throws InterruptedException {
        SiddhiManager siddhiManager = new SiddhiManager();

        String cseEventStream = "" + "define stream cseEventStream (symbol string, price float, volume int);";
        String query = "" + "@info(name = 'query1') " + "from cseEventStream#throttler:timeBatch(1 min , 0) "
                + "select symbol,sum(price) as sumPrice,volume, expiryTimeStamp "
                + "insert all events into outputStream ;";

        SiddhiAppRuntime executionPlanRuntime = siddhiManager.createSiddhiAppRuntime(cseEventStream + query);

        executionPlanRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timeStamp, inEvents, removeEvents);
                if (inEvents != null) {
                    inEventCount = inEventCount + inEvents.length;
                    lastCurrentEvent = inEvents[inEvents.length - 1];
                } else if (removeEvents != null) {
                    removeEventCount = removeEventCount + removeEvents.length;
                    lastRemoveEvent = removeEvents[removeEvents.length - 1];
                }
                eventArrived = true;
            }

        });

        InputHandler inputHandler = executionPlanRuntime.getInputHandler("cseEventStream");
        executionPlanRuntime.start();
        inputHandler.send(new Object[] { "IBM", 700f, 0 });
        inputHandler.send(new Object[] { "WSO2", 60.5f, 1 });
        Thread.sleep(260000);
        inputHandler.send(new Object[] { "IBM", 700f, 0 });
        Assert.assertEquals(3, inEventCount);
        Assert.assertTrue("Event expiry time is not valid for the current batch",
                (Long) (lastCurrentEvent.getData()[3]) >= System.currentTimeMillis());
        Assert.assertTrue(eventArrived);
        executionPlanRuntime.shutdown();

    }

}
