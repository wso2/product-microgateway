/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2am.micro.gw.mockconsul;

import java.util.Arrays;

public class Node {
    private String id;
    private Datacenter datacenter;
    private Service service;
    private String address;
    private int port;
    private String[] tags;
    private HealthCheck healthCheck;
    private ConsulNode consulNode;

    public Node(String id, Datacenter datacenter, Service service, String address, int port, String[] tags, HealthCheck healthCheck, ConsulNode consulNode) {
        this.id = id;
        this.datacenter = datacenter;
        this.service = service;
        this.address = address;
        this.port = port;
        this.tags = tags;
        this.healthCheck = healthCheck;
        this.consulNode = consulNode;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ConsulNode getConsulNode() {
        return consulNode;
    }

    public void setConsulNode(ConsulNode consulNode) {
        this.consulNode = consulNode;
    }

    public HealthCheck getHealthCheck() {
        return healthCheck;
    }

    public void setHealthCheck(HealthCheck healthCheck) {
        this.healthCheck = healthCheck;
    }

    public Datacenter getDatacenter() {
        return datacenter;
    }

    public void setDatacenter(Datacenter datacenter) {
        this.datacenter = datacenter;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "Node{" +
                "datacenter=" + datacenter +
                ", service=" + service +
                ", address='" + address + '\'' +
                ", port=" + port +
                ", tags=" + Arrays.toString(tags) +
                ", healthCheck=" + healthCheck +
                '}';
    }
}
