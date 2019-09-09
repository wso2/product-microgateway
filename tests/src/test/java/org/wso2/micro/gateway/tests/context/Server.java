/*
 * Copyright (c)  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.micro.gateway.tests.context;

/**
 * Interface for test Server implementation.
 */
public interface Server {

    /**
     * Start micro-gw server passing provided .bal files.
     *
     * @throws MicroGWTestException if services start fails
     */
    public void startServer() throws MicroGWTestException;

    /**
     * Stops the server started by startServer method.
     *
     * @throws MicroGWTestException if rest stop fails
     */
    public void stopServer(boolean deleteExtractedDir) throws MicroGWTestException;

    /**
     * Stop the server and start it again.
     *
     * @throws MicroGWTestException if restart fails
     */
    public void restartServer() throws MicroGWTestException;

    /**
     * Checks if the server is already running.
     *
     * @return True if the server is running
     */
    public boolean isRunning();
}

