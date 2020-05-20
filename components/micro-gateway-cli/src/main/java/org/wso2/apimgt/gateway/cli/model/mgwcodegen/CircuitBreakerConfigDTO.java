/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.apimgt.gateway.cli.model.mgwcodegen;

import java.util.ArrayList;
import java.util.List;

/**
 * This DTO holds the data related for the circuit breaking.
 */
public class CircuitBreakerConfigDTO {
    private RollingWindowConfigDTO rollingWindow;
    private double failureThreshold = 0.0;
    private int resetTimeInMillis = 0;
    private List<Integer> statusCodes = new ArrayList<>();

    public RollingWindowConfigDTO getRollingWindow() {
        return rollingWindow;
    }

    public void setRollingWindow(RollingWindowConfigDTO rollingWindow) {
        this.rollingWindow = rollingWindow;
    }

    public double getFailureThreshold() {
        return failureThreshold;
    }

    public void setFailureThreshold(double failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    public int getResetTimeInMillis() {
        return resetTimeInMillis;
    }

    public void setResetTimeInMillis(int resetTimeInMillis) {
        this.resetTimeInMillis = resetTimeInMillis;
    }

    public List<Integer> getStatusCodes() {
        return statusCodes;
    }

    public void setStatusCodes(List<Integer> statusCodes) {
        this.statusCodes = statusCodes;
    }
}
