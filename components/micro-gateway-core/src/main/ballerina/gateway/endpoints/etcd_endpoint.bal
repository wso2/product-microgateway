import ballerina/io;
import ballerina/http;
import ballerina/config;

endpoint http:Client etcdEndpoint {
    url: retrieveConfig("etcdurl", "http://127.0.0.1:2379")
};
