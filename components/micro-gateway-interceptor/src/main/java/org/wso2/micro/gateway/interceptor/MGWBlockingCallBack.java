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
package org.wso2.micro.gateway.interceptor;

import org.ballerinalang.jvm.scheduling.Scheduler;
import org.ballerinalang.jvm.scheduling.Strand;
import org.ballerinalang.jvm.values.ErrorValue;
import org.ballerinalang.jvm.values.connector.NonBlockingCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Represents a blocking call back when request payloads are used.
 */
public class MGWBlockingCallBack extends NonBlockingCallback {

    private static final Logger log = LoggerFactory.getLogger("ballerina");

    private volatile Semaphore executionWaitSem;
    private final Strand strand;
    private final Scheduler scheduler;

    public MGWBlockingCallBack(Strand strand) {
        super(strand);
        this.strand = strand;
        this.scheduler = strand.scheduler;
        executionWaitSem = new Semaphore(0);
    }

    @Override
    public void notifySuccess() {
        super.notifySuccess();
        executionWaitSem.release();
    }

    @Override
    public void notifyFailure(ErrorValue error) {
        super.notifyFailure(error);
        executionWaitSem.release();
    }

    public void notifyFailure() {
        this.scheduler.unblockStrand(strand);
    }

    public void sync() {
        try {
            executionWaitSem.tryAcquire(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Error while synchronously building the payload", e);
        }
    }

}
