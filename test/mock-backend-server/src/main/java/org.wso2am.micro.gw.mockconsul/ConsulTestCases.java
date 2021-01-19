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

import java.util.HashMap;
import java.util.Map;

public class ConsulTestCases {
    public static Map<String, ConsulState> testCases = new HashMap<>();

    public static void loadTestCases() {
        testCases.put("1", consulServer -> {
            Datacenter localDc = new Datacenter("local-dc");
            Datacenter dc1 = new Datacenter("dc1");
            ConsulNode cn = new ConsulNode("d674a962-2e8e-d765-aa91-6d4d66fecacb", "machine", "127.0.0.1");
            ConsulNode cn1 = new ConsulNode("d675a962-278e-d865-aa61-6d4d66feaabc", "machine2", "192.168.43.1");
            Service webService = new Service("web", "");
            Service pizzaService = new Service("pizza", "");


            HealthCheck h = new HealthCheck();
            String[] tags = {"production"};
            Node n = new Node(Helper.generateRandomChars(), localDc, webService, "127.0.0.1", 3000, tags, h, cn);
            consulServer.addNode(n);

            //another dc
            HealthCheck h1 = new HealthCheck();
            String[] tags1 = {"production"};
            Node n1 = new Node(Helper.generateRandomChars(), dc1, webService, "127.0.0.1", 8080, tags1, h1, cn1);
            consulServer.addNode(n1);

            //another service
            HealthCheck h2 = new HealthCheck();
            String[] tags2 = {"production"};
            Node n2 = new Node(Helper.generateRandomChars(), localDc, pizzaService, "127.0.0.1", 5000, tags2, h2, cn);
            consulServer.addNode(n2);

            //another tag
            HealthCheck h3 = new HealthCheck();
            String[] tags3 = {"dev"};
            Node n3 = new Node(Helper.generateRandomChars(), localDc, webService, "127.0.0.1", 6000, tags3, h3, cn1);
            consulServer.addNode(n3);

            //health check critical
            HealthCheck h4 = new HealthCheck();
            h4.SetCritical();
            String[] tags4 = {"production"};
            Node n4 = new Node(Helper.generateRandomChars(), localDc, webService, "127.0.0.1", 7000, tags4, h4, cn);
            consulServer.addNode(n4);

            HealthCheck h5 = new HealthCheck();
            String[] tags5 = {"production"};
            Node n5 = new Node(Helper.generateRandomChars(), localDc, webService, "127.0.0.1", 4000, tags5, h5, cn1);
            consulServer.addNode(n5);

        });

        testCases.put("2", consulServer -> {
            Datacenter localDc = new Datacenter("local-dc");
            Datacenter dc1 = new Datacenter("dc1");
            ConsulNode cn = new ConsulNode("d674a962-2e8e-d765-aa91-6d4d66fecacb", "machine", "127.0.0.1");
            ConsulNode cn1 = new ConsulNode("d675a962-278e-d865-aa61-6d4d66feaabc", "machine2", "192.168.43.1");
            Service webService = new Service("web", "");


            HealthCheck h = new HealthCheck();
            String[] tags = {"production"};
            Node n = new Node(Helper.generateRandomChars(), localDc, webService, "127.0.0.1", 3000, tags, h, cn);
            consulServer.addNode(n);

            //another dc
            HealthCheck h1 = new HealthCheck();
            String[] tags1 = {"production"};
            Node n1 = new Node(Helper.generateRandomChars(), dc1, webService, "127.0.0.1", 8080, tags1, h1, cn1);
            consulServer.addNode(n1);

            //health check critical->passed
            HealthCheck h4 = new HealthCheck();
            String[] tags4 = {"production"};
            Node n4 = new Node(Helper.generateRandomChars(), localDc, webService, "127.0.0.1", 7000, tags4, h4, cn);
            consulServer.addNode(n4);

            //remove an old node, introduce a new node
            HealthCheck h5 = new HealthCheck();
            String[] tags5 = {"production"};
            Node n5 = new Node(Helper.generateRandomChars(), localDc, webService, "127.0.0.1", 4500, tags5, h5, cn1);
            consulServer.addNode(n5);
        });

        testCases.put("3", consulServer -> {
            consulServer.stopServer();
        });
    }
}
