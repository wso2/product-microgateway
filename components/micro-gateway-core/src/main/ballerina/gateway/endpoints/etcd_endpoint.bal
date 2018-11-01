import ballerina/io;
import ballerina/http;
import ballerina/config;

endpoint http:Client etcdEndpoint {
    url: config:getAsString("etcdurl", default="http://10.100.4.209:2379")
};
