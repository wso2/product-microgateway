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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ConsulServer starts an HTTP server to mock the behaviour of a Consul Client/Server
 */
public class ConsulServer extends Thread {
    private static final Logger logger = Logger.getLogger(ConsulServer.class.getName());
    private final String host; //localhost
    private final int port;
    private final String scheme; //http, https
    private List<Node> nodes = new ArrayList<>(); //list of service nodes
    private final Map<String, String> responseHeaders = new HashMap<>();
    private HttpServer httpServer;


    public ConsulServer(String host, int port, String scheme) {
        this.host = host;
        this.port = port;
        this.scheme = scheme;

        responseHeaders.put("Content-Type", "application/json");
        responseHeaders.put("Vary", "Accept-Encoding");
        responseHeaders.put("X-Consul-Default-Acl-Policy", "allow");
        responseHeaders.put("X-Consul-Effective-Consistency", "leader");
        responseHeaders.put("X-Consul-Index", "101");
        responseHeaders.put("X-Consul-Knownleader", "true");
        responseHeaders.put("X-Consul-Lastcontact", "0");
        responseHeaders.put("Date", "");//todo add date
    }


    public void addNode(Node node) {
        nodes.add(node);
    }

    public void removeNode(Node node) {
        nodes.remove(node);
    }

    public void resetServer() {
        nodes = new ArrayList<>();
    }

    public void stopServer() {
        httpServer.stop(0);
    }

    /**
     * Filters the available nodes by service name, datacenter and health check status
     *
     * @param datacenter          name of the datacenter
     * @param serviceName         name of the Service
     * @param healthChecksPassing status of health check
     * @return List of Nodes matching the criteria
     */
    public List<Node> get(String datacenter, String serviceName, boolean healthChecksPassing) {
        List<Node> nodes = new ArrayList<>();
        for (Node node : get(datacenter, serviceName)) {
            if (healthChecksPassing == node.getHealthCheck().isPassing()) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    /**
     * Filters the available nodes by service name and datacenter
     *
     * @param datacenter  name of the datacenter
     * @param serviceName name of the Service
     * @return List of Nodes matching the criteria
     * @see #get(String)
     * @see #get(String, String, boolean)
     */
    public List<Node> get(String datacenter, String serviceName) {
        List<Node> ret = new ArrayList<>();
        for (Node node : get(serviceName)) {
            if (node.getDatacenter().getName().equals(datacenter)) {
                ret.add(node);
            }
        }
        return ret;
    }

    /**
     * Filters the available nodes by service name
     *
     * @param serviceName name of the Service
     * @return List of Nodes matching the criteria
     * @see #get(String, String)
     * @see #get(String, String, boolean)
     */
    public List<Node> get(String serviceName) {
        List<Node> ret = new ArrayList<>();
        for (Node node : nodes) {
            if (node.getService().getName().equals(serviceName)) {
                ret.add(node);
            }
        }
        return ret;
    }

    private void setHTTPResponseHeaders(HttpExchange exchange) {
        for (String k : responseHeaders.keySet()) {
            exchange.getResponseHeaders().set(k, responseHeaders.get(k));
        }
    }

    private void sendHTTPResponse(HttpExchange exchange, byte[] response) throws IOException {
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    /**
     * Splits the URI into path param and query params.
     * Splits only one path param.
     * Splits all the query params.
     *
     * @param base base of the URI
     * @param uri  complete URI(without the domain and port)
     * @return Map that contains 1 path param and all the query params
     */
    private Map<String, String> paramsAndQueryToMap(String base, String uri) {
        Map<String, String> result = new HashMap<>();
        String[] rest = uri.split(base);
        if (rest.length != 2) {
            return result;
        }
        String nameAndQuery = rest[1];
        String[] sp = nameAndQuery.split("\\?");
        String pathParam = sp[0];
        result.put("PATH_PARAM", pathParam);
        if (sp.length != 2) {
            return result;
        }
        String query = sp[1];
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            } else {
                result.put(entry[0], "");
            }
        }
        return result;
    }

    /**
     * Creates a JSON String for a given Node
     *
     * @param node Node which the JSON for response is created.
     * @return String JSON for the response.
     */
    private String buildJsonForNode(Node node) {
        System.out.println(node);
        String jsonTemplate = "{\n" +
                "        \"Node\": {\n" +
                "            \"ID\": \"${nodeId}\",\n" +
                "            \"Node\": \"${nodeName}\",\n" +
                "            \"Address\": \"${consulNodeAddress}\",\n" +
                "            \"Datacenter\": \"${datacenter}\",\n" +
                "            \"TaggedAddresses\": {\n" +
                "                \"lan\": \"${consulNodeAddress}\",\n" +
                "                \"lan_ipv4\": \"${consulNodeAddress}\",\n" +
                "                \"wan\": \"${consulNodeAddress}\",\n" +
                "                \"wan_ipv4\": \"${consulNodeAddress}\"\n" +
                "            },\n" +
                "            \"Meta\": {\n" +
                "                \"consul-network-segment\": \"\"\n" +
                "            },\n" +
                "            \"CreateIndex\": ${createIndex},\n" +
                "            \"ModifyIndex\": ${createIndex}\n" +
                "        },\n" +
                "        \"Service\": {\n" +
                "            \"ID\": \"${serviceId}\",\n" +
                "            \"Service\": \"${serviceName}\",\n" +
                "            \"Tags\": [\n" +
                "                \"${tag}\"\n" +
                "            ],\n" +
                "            \"Address\": \"${address}\",\n" +
                "            \"Meta\": null,\n" +
                "            \"Port\": ${port},\n" +
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
                "            \"CreateIndex\": 14,\n" +
                "            \"ModifyIndex\": 14\n" +
                "        },\n" +
                "        \"Checks\": [\n" +
                "            {\n" +
                "                \"Node\": \"${nodeName}\",\n" +
                "                \"CheckID\": \"api 3000\",\n" +
                "                \"Name\": \"health check on 3000\",\n" +
                "                \"Status\": \"${healthStatus}\",\n" +
                "                \"Notes\": \"\",\n" +
                "                \"Output\": \"Get \\\"http://localhost:3000\\\": dial tcp 127.0.0.1:3000: connect: connection refused\",\n" +
                "                \"ServiceID\": \"3000l\",\n" +
                "                \"ServiceName\": \"web\",\n" +
                "                \"ServiceTags\": [\n" +
                "                    \"golang\"\n" +
                "                ],\n" +
                "                \"Type\": \"http\",\n" +
                "                \"Definition\": {},\n" +
                "                \"CreateIndex\": 14,\n" +
                "                \"ModifyIndex\": 101\n" +
                "            }\n" +
                "        ]\n" +
                "    },";

        //todo replace string variables with: org.apache.commons.text.StringSubstitutor
        //<dependency>
        //   <groupId>org.apache.commons</groupId>
        //   <artifactId>commons-text</artifactId>
        //   <version>1.9</version>
        //</dependency>
        jsonTemplate = jsonTemplate.replace("${nodeId}", node.getConsulNode().getNodeId());
        jsonTemplate = jsonTemplate.replace("${nodeName}", node.getConsulNode().getNodeName());
        jsonTemplate = jsonTemplate.replace("${nodeName}", node.getConsulNode().getNodeName());
        jsonTemplate = jsonTemplate.replace("${datacenter}", node.getDatacenter().getName());

        jsonTemplate = jsonTemplate.replace("${consulNodeAddress}", node.getConsulNode().getAddress());
        jsonTemplate = jsonTemplate.replace("${consulNodeAddress}", node.getConsulNode().getAddress());
        jsonTemplate = jsonTemplate.replace("${consulNodeAddress}", node.getConsulNode().getAddress());
        jsonTemplate = jsonTemplate.replace("${consulNodeAddress}", node.getConsulNode().getAddress());
        jsonTemplate = jsonTemplate.replace("${consulNodeAddress}", node.getConsulNode().getAddress());

        jsonTemplate = jsonTemplate.replace("${address}", node.getAddress());

        jsonTemplate = jsonTemplate.replace("${createIndex}", "50");
        jsonTemplate = jsonTemplate.replace("${createIndex}", "52");
        jsonTemplate = jsonTemplate.replace("${serviceId}", node.getId());
        jsonTemplate = jsonTemplate.replace("${serviceName}", node.getService().getName());
        jsonTemplate = jsonTemplate.replace("${port}", Integer.toString(node.getPort()));
        jsonTemplate = jsonTemplate.replace("${tag}", node.getTags()[0]);//todo multiple tags
        jsonTemplate = jsonTemplate.replace("${healthStatus}", node.getHealthCheck().getStatus());

        return jsonTemplate;
    }

    /**
     * starts the mock HTTP server.
     * /v1/health/service/ is where the adapter queries.
     * /tc/ is where the integration tests send HTTP requests to change the state of the mock consul server.
     */
    @Override
    public void run() {
        try {
            if (scheme.equals("https")) {
                this.httpServer = HttpsServer.create(new InetSocketAddress(host, port), 0);
                //todo https
            } else {
                this.httpServer = HttpServer.create(new InetSocketAddress(host, port), 0);
            }


            String consulContext = "/v1/health/service/";
            httpServer.createContext(consulContext, exchange -> {
                System.out.println(exchange.getRequestURI());
                Map<String, String> map = paramsAndQueryToMap(consulContext, exchange.getRequestURI().toString());
                String serviceName = map.get("PATH_PARAM");
                //dc=local-dc&passing=1
                List<Node> resultNodes = get(serviceName);
                if (map.containsKey("dc")) {
                    String dc = map.get("dc");
                    resultNodes = get(dc, serviceName);
                    if (map.containsKey("passing")) {
                        resultNodes = get(dc, serviceName, true);
                    }
                }
                byte[] response = buildAllResultsToJsonArray(resultNodes).getBytes();
//                response = phonyResponse().getBytes();
                setHTTPResponseHeaders(exchange);
                sendHTTPResponse(exchange, response);
                exchange.getResponseBody().write(response);
                exchange.close();

            });

            String testCasesContext = "/tc/";
            httpServer.createContext(testCasesContext, exchange -> {
                System.out.println(exchange.getRequestURI());
                Map<String, String> map = paramsAndQueryToMap(testCasesContext, exchange.getRequestURI().toString());
                String testCase = map.get("PATH_PARAM");
                //call methods to change consul server state
                resetServer(); //reset the state before loading a new state
                if (ConsulTestCases.testCases.containsKey(testCase)) {
                    ConsulTestCases.testCases.get(testCase).loadState(this);
                    sendHTTPResponse(exchange, testCase.getBytes());
                } else {
                    logger.log(Level.SEVERE, "Test case not found: " + testCase);
                    sendHTTPResponse(exchange, ("Test case not found: " + testCase).getBytes());
                }
            });
            httpServer.start();
            logger.log(Level.INFO, "Consul mock server started: " + this + scheme + "://" + this.host + ":" + this.port);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error starting the consul mock server: ", e);
        }
    }

    /**
     * Gives the output JSON array to the given set of Nodes
     *
     * @param nodeArrayList List of Nodes
     * @return String JSON array
     */
    private String buildAllResultsToJsonArray(List<Node> nodeArrayList) {
        StringBuilder sb = new StringBuilder();
        for (Node node : nodeArrayList) {
            sb.append(this.buildJsonForNode(node));
        }
        sb.setLength(sb.length() - 1); // remove the trailing ","
        sb.insert(0, "[");
        sb.append("]");
        return sb.toString();
    }


    public static void main(String[] args) {
        String hostAddress = "0.0.0.0";//127.0.0.1 isn't exposed to outside of docker
        ConsulServer consulServer = new ConsulServer(hostAddress, 8500, "http");
        consulServer.start();
        ConsulTestCases.loadTestCases(); //load test cases
    }
}