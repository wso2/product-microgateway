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

import java.util.Random;

public class Helper {
    public static String generateRandomChars() {
        String candidateChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        int length = 5;
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(candidateChars.charAt(random.nextInt(candidateChars.length())));
        }

        return sb.toString();
    }

    public static int getRandomNumber() {
        return (int) (Math.random() * 50 + 1);
    }

    public static String phonyResponse() {
        return "[\n" +
                "    {\n" +
                "        \"Node\": {\n" +
                "            \"ID\": \"d674a962-2e8e-d765-aa91-6d4d66fecacb\",\n" +
                "            \"Node\": \"machine\",\n" +
                "            \"Address\": \"127.0.0.1\",\n" +
                "            \"Datacenter\": \"local-dc\",\n" +
                "            \"TaggedAddresses\": {\n" +
                "                \"lan\": \"127.0.0.1\",\n" +
                "                \"lan_ipv4\": \"127.0.0.1\",\n" +
                "                \"wan\": \"127.0.0.1\",\n" +
                "                \"wan_ipv4\": \"127.0.0.1\"\n" +
                "            },\n" +
                "            \"Meta\": {\n" +
                "                \"consul-network-segment\": \"\"\n" +
                "            },\n" +
                "            \"CreateIndex\": 6,\n" +
                "            \"ModifyIndex\": 7\n" +
                "        },\n" +
                "        \"Service\": {\n" +
                "            \"ID\": \"3000l\",\n" +
                "            \"Service\": \"web\",\n" +
                "            \"Tags\": [\n" +
                "                \"golang\"\n" +
                "            ],\n" +
                "            \"Address\": \"\",\n" +
                "            \"Meta\": null,\n" +
                "            \"Port\": 3000,\n" +
                "            \"Weights\": {\n" +
                "                \"Passing\": 1,\n" +
                "                \"Warning\": 1\n" +
                "            },\n" +
                "            \"EnableTagOverride\": false,\n" +
                "            \"Proxy\": {\n" +
                "                \"MeshGateway\": {},\n" +
                "                \"Expose\": {}\n" +
                "            },\n" +
                "            \"Connect\": {},\n" +
                "            \"CreateIndex\": 11,\n" +
                "            \"ModifyIndex\": 11\n" +
                "        },\n" +
                "        \"Checks\": [\n" +
                "            {\n" +
                "                \"Node\": \"machine\",\n" +
                "                \"CheckID\": \"serfHealth\",\n" +
                "                \"Name\": \"Serf Health Status\",\n" +
                "                \"Status\": \"passing\",\n" +
                "                \"Notes\": \"\",\n" +
                "                \"Output\": \"Agent alive and reachable\",\n" +
                "                \"ServiceID\": \"\",\n" +
                "                \"ServiceName\": \"\",\n" +
                "                \"ServiceTags\": [],\n" +
                "                \"Type\": \"\",\n" +
                "                \"Definition\": {},\n" +
                "                \"CreateIndex\": 6,\n" +
                "                \"ModifyIndex\": 6\n" +
                "            },\n" +
                "            {\n" +
                "                \"Node\": \"machine\",\n" +
                "                \"CheckID\": \"api 3000\",\n" +
                "                \"Name\": \"health check on 3000\",\n" +
                "                \"Status\": \"critical\",\n" +
                "                \"Notes\": \"\",\n" +
                "                \"Output\": \"\",\n" +
                "                \"ServiceID\": \"3000l\",\n" +
                "                \"ServiceName\": \"web\",\n" +
                "                \"ServiceTags\": [\n" +
                "                    \"golang\"\n" +
                "                ],\n" +
                "                \"Type\": \"http\",\n" +
                "                \"Definition\": {},\n" +
                "                \"CreateIndex\": 11,\n" +
                "                \"ModifyIndex\": 11\n" +
                "            }\n" +
                "        ]\n" +
                "    },\n" +
                "    {\n" +
                "        \"Node\": {\n" +
                "            \"ID\": \"d674a962-2e8e-d765-aa91-6d4d66fecacb\",\n" +
                "            \"Node\": \"machine\",\n" +
                "            \"Address\": \"127.0.0.1\",\n" +
                "            \"Datacenter\": \"local-dc\",\n" +
                "            \"TaggedAddresses\": {\n" +
                "                \"lan\": \"127.0.0.1\",\n" +
                "                \"lan_ipv4\": \"127.0.0.1\",\n" +
                "                \"wan\": \"127.0.0.1\",\n" +
                "                \"wan_ipv4\": \"127.0.0.1\"\n" +
                "            },\n" +
                "            \"Meta\": {\n" +
                "                \"consul-network-segment\": \"\"\n" +
                "            },\n" +
                "            \"CreateIndex\": 6,\n" +
                "            \"ModifyIndex\": 7\n" +
                "        },\n" +
                "        \"Service\": {\n" +
                "            \"ID\": \"5000l\",\n" +
                "            \"Service\": \"web\",\n" +
                "            \"Tags\": [\n" +
                "                \"golang\",\n" +
                "                \"server3\",\n" +
                "                \"has sidecar\"\n" +
                "            ],\n" +
                "            \"Address\": \"\",\n" +
                "            \"Meta\": null,\n" +
                "            \"Port\": 5000,\n" +
                "            \"Weights\": {\n" +
                "                \"Passing\": 1,\n" +
                "                \"Warning\": 1\n" +
                "            },\n" +
                "            \"EnableTagOverride\": false,\n" +
                "            \"Proxy\": {\n" +
                "                \"MeshGateway\": {},\n" +
                "                \"Expose\": {}\n" +
                "            },\n" +
                "            \"Connect\": {},\n" +
                "            \"CreateIndex\": 13,\n" +
                "            \"ModifyIndex\": 13\n" +
                "        },\n" +
                "        \"Checks\": [\n" +
                "            {\n" +
                "                \"Node\": \"machine\",\n" +
                "                \"CheckID\": \"serfHealth\",\n" +
                "                \"Name\": \"Serf Health Status\",\n" +
                "                \"Status\": \"passing\",\n" +
                "                \"Notes\": \"\",\n" +
                "                \"Output\": \"Agent alive and reachable\",\n" +
                "                \"ServiceID\": \"\",\n" +
                "                \"ServiceName\": \"\",\n" +
                "                \"ServiceTags\": [],\n" +
                "                \"Type\": \"\",\n" +
                "                \"Definition\": {},\n" +
                "                \"CreateIndex\": 6,\n" +
                "                \"ModifyIndex\": 6\n" +
                "            }\n" +
                "        ]\n" +
                "    },\n" +
                "    {\n" +
                "        \"Node\": {\n" +
                "            \"ID\": \"d674a962-2e8e-d765-aa91-6d4d66fecacb\",\n" +
                "            \"Node\": \"machine\",\n" +
                "            \"Address\": \"127.0.0.1\",\n" +
                "            \"Datacenter\": \"local-dc\",\n" +
                "            \"TaggedAddresses\": {\n" +
                "                \"lan\": \"127.0.0.1\",\n" +
                "                \"lan_ipv4\": \"127.0.0.1\",\n" +
                "                \"wan\": \"127.0.0.1\",\n" +
                "                \"wan_ipv4\": \"127.0.0.1\"\n" +
                "            },\n" +
                "            \"Meta\": {\n" +
                "                \"consul-network-segment\": \"\"\n" +
                "            },\n" +
                "            \"CreateIndex\": 6,\n" +
                "            \"ModifyIndex\": 7\n" +
                "        },\n" +
                "        \"Service\": {\n" +
                "            \"ID\": \"8080l\",\n" +
                "            \"Service\": \"web\",\n" +
                "            \"Tags\": [\n" +
                "                \"golang\",\n" +
                "                \"server2\"\n" +
                "            ],\n" +
                "            \"Address\": \"\",\n" +
                "            \"Meta\": null,\n" +
                "            \"Port\": 8080,\n" +
                "            \"Weights\": {\n" +
                "                \"Passing\": 1,\n" +
                "                \"Warning\": 1\n" +
                "            },\n" +
                "            \"EnableTagOverride\": false,\n" +
                "            \"Proxy\": {\n" +
                "                \"MeshGateway\": {},\n" +
                "                \"Expose\": {}\n" +
                "            },\n" +
                "            \"Connect\": {},\n" +
                "            \"CreateIndex\": 12,\n" +
                "            \"ModifyIndex\": 12\n" +
                "        },\n" +
                "        \"Checks\": [\n" +
                "            {\n" +
                "                \"Node\": \"machine\",\n" +
                "                \"CheckID\": \"serfHealth\",\n" +
                "                \"Name\": \"Serf Health Status\",\n" +
                "                \"Status\": \"passing\",\n" +
                "                \"Notes\": \"\",\n" +
                "                \"Output\": \"Agent alive and reachable\",\n" +
                "                \"ServiceID\": \"\",\n" +
                "                \"ServiceName\": \"\",\n" +
                "                \"ServiceTags\": [],\n" +
                "                \"Type\": \"\",\n" +
                "                \"Definition\": {},\n" +
                "                \"CreateIndex\": 6,\n" +
                "                \"ModifyIndex\": 6\n" +
                "            },\n" +
                "            {\n" +
                "                \"Node\": \"machine\",\n" +
                "                \"CheckID\": \"api 8080\",\n" +
                "                \"Name\": \"health check on 8080\",\n" +
                "                \"Status\": \"passing\",\n" +
                "                \"Notes\": \"\",\n" +
                "                \"Output\": \"HTTP GET http://localhost:8080: 200 OK Output: {\\\"body\\\":{},\\\"url\\\":\\\"/\\\",\\\"params\\\":{\\\"0\\\":\\\"/\\\"},\\\"query\\\":{},\\\"ip\\\":\\\"::ffff:172.17.0.1\\\"}\",\n" +
                "                \"ServiceID\": \"8080l\",\n" +
                "                \"ServiceName\": \"web\",\n" +
                "                \"ServiceTags\": [\n" +
                "                    \"golang\",\n" +
                "                    \"server2\"\n" +
                "                ],\n" +
                "                \"Type\": \"http\",\n" +
                "                \"Definition\": {},\n" +
                "                \"CreateIndex\": 12,\n" +
                "                \"ModifyIndex\": 15\n" +
                "            }\n" +
                "        ]\n" +
                "    },\n" +
                "    {\n" +
                "        \"Node\": {\n" +
                "            \"ID\": \"d674a962-2e8e-d765-aa91-6d4d66fecacb\",\n" +
                "            \"Node\": \"machine\",\n" +
                "            \"Address\": \"127.0.0.1\",\n" +
                "            \"Datacenter\": \"local-dc\",\n" +
                "            \"TaggedAddresses\": {\n" +
                "                \"lan\": \"127.0.0.1\",\n" +
                "                \"lan_ipv4\": \"127.0.0.1\",\n" +
                "                \"wan\": \"127.0.0.1\",\n" +
                "                \"wan_ipv4\": \"127.0.0.1\"\n" +
                "            },\n" +
                "            \"Meta\": {\n" +
                "                \"consul-network-segment\": \"\"\n" +
                "            },\n" +
                "            \"CreateIndex\": 6,\n" +
                "            \"ModifyIndex\": 7\n" +
                "        },\n" +
                "        \"Service\": {\n" +
                "            \"ID\": \"web5435\",\n" +
                "            \"Service\": \"web\",\n" +
                "            \"Tags\": [\n" +
                "                \"react\",\n" +
                "                \"front\"\n" +
                "            ],\n" +
                "            \"Address\": \"\",\n" +
                "            \"Meta\": null,\n" +
                "            \"Port\": 3001,\n" +
                "            \"Weights\": {\n" +
                "                \"Passing\": 1,\n" +
                "                \"Warning\": 1\n" +
                "            },\n" +
                "            \"EnableTagOverride\": false,\n" +
                "            \"Proxy\": {\n" +
                "                \"MeshGateway\": {},\n" +
                "                \"Expose\": {}\n" +
                "            },\n" +
                "            \"Connect\": {},\n" +
                "            \"CreateIndex\": 10,\n" +
                "            \"ModifyIndex\": 10\n" +
                "        },\n" +
                "        \"Checks\": [\n" +
                "            {\n" +
                "                \"Node\": \"machine\",\n" +
                "                \"CheckID\": \"serfHealth\",\n" +
                "                \"Name\": \"Serf Health Status\",\n" +
                "                \"Status\": \"passing\",\n" +
                "                \"Notes\": \"\",\n" +
                "                \"Output\": \"Agent alive and reachable\",\n" +
                "                \"ServiceID\": \"\",\n" +
                "                \"ServiceName\": \"\",\n" +
                "                \"ServiceTags\": [],\n" +
                "                \"Type\": \"\",\n" +
                "                \"Definition\": {},\n" +
                "                \"CreateIndex\": 6,\n" +
                "                \"ModifyIndex\": 6\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "]";
    }
}
