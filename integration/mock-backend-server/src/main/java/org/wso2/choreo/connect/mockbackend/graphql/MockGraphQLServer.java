/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.choreo.connect.mockbackend.graphql;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.choreo.connect.mockbackend.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class MockGraphQLServer extends Thread {
    private static final Logger log = LoggerFactory.getLogger(MockGraphQLServer.class);
    private final int gqlServerPort;

    public MockGraphQLServer(int port) {
        gqlServerPort = port;
    }

    public void run() {
        if (gqlServerPort < 0) {
            throw new RuntimeException("GraphQL server port not defined.");
        } else {
            log.info("Starting MockGraphQL server on port: {}.", gqlServerPort);
        }
        try {
            HttpServer gqlServer = HttpServer.create(new InetSocketAddress(gqlServerPort), 0);
            gqlServer.createContext("/gql", new GraphQLServerHandler());
            gqlServer.start();
        } catch (IOException e) {
            log.error("Error occurred while starting Mock GraphQL server. Error: {}", e.getMessage());
        }
    }

    static class GraphQLServerHandler implements HttpHandler {
        private String jsonKeyForQuery = "query";
        private String jsonKeyForOperationName = "operationName";
        // obtains this file from CC choreo-connect-mock-backend container
        File gqlSchemaFile = new File("/tmp/schema.graphql");

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestBody = IOUtils.toString(exchange.getRequestBody(), StandardCharsets.UTF_8);
            if (requestBody.contains(jsonKeyForQuery)) {
                JSONObject jsonOjb = new JSONObject(requestBody);
                requestBody = jsonOjb.get(jsonKeyForQuery).toString();
                String gqlOperationName = "";
                if (!jsonOjb.isNull(jsonKeyForOperationName)) {
                    gqlOperationName = jsonOjb.get(jsonKeyForOperationName).toString();
                    requestBody = requestBody.replaceAll(gqlOperationName, "");
                }
            }
            InputStream in = new FileInputStream(gqlSchemaFile);
            String schema = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            SchemaParser schemaParser = new SchemaParser();
            TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

            Map<String, Hero> heroes = new LinkedHashMap<>();
            heroes.put("hero-1", new Hero("hero-1", 10, new Address("Earth", "LA")));
            Map<String, Address> addresses = new LinkedHashMap<>();
            addresses.put("address-1", new Address("Earth", "LA"));

            DataFetcher heroQueryDataFetcher = environment -> new ArrayList<>(heroes.values());
            DataFetcher locationDataFetcher = environment -> new ArrayList<>(addresses.values());

            DataFetcher dataFetcher = new DataFetcher() {
                @Override
                public Object get(DataFetchingEnvironment environment) {
                    String name = environment.getArgument("name");
                    int age = environment.getArgument("age");
                    Address location = environment.getArgument("location");
                    String key = name + age;
                    heroes.put(key, new Hero(name, age, location));
                    return new Hero(environment.getArgument("name"), environment.getArgument("age"),
                            environment.getArgument("location"));
                }
            };

            RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                    .type("Query", builder -> builder.dataFetcher("hero", heroQueryDataFetcher))
                    .type("Query", builder -> builder.dataFetcher("address", locationDataFetcher))
                    .type("Mutation", builder -> builder.dataFetcher("createHero", dataFetcher))
                    .build();

            SchemaGenerator schemaGenerator = new SchemaGenerator();
            GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
            GraphQL build = GraphQL.newGraphQL(graphQLSchema).build();
            ExecutionResult executionResult = build.execute(requestBody);
            if (executionResult.getErrors().size() > 0) {
                log.error("Error occurred while GQL query processing {}", executionResult.getErrors());
            }

            JSONObject responseJSON = new JSONObject();
            responseJSON.put("data", executionResult.getData().toString());
            byte[] response = responseJSON.toString().getBytes();
            Utils.respondWithBodyAndClose(HttpURLConnection.HTTP_OK, response, exchange);
        }
    }

    static class Hero {
        private final String name;
        private final int age;
        private final Address location;

        public Hero(String name, int age, Address location) {
            this.name = name;
            this.age = age;
            this.location = location;
        }
    }

    static class Address {
        private final String planet;
        private final String village;

        public Address(String planet, String village) {
            this.planet = planet;
            this.village = village;
        }
    }
}
