// Copyright (c)  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file   except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/http;
import ballerina/log;
import ballerina/auth;
import ballerina/config;
import ballerina/runtime;
import ballerina/system;
import ballerina/time;
import ballerina/io;
import ballerina/reflect;
import ballerina/swagger;
import wso2/gateway;

endpoint http:Client PizzaShackAPI_1_0_0_EP {
    url: "https://localhost:9443/am/sample/pizzashack/v1/api/",
    cache: { enabled: false }
    
};

ExtensionFilter extensionFilter;

endpoint gateway:APIGatewayListener apiListener {
    filters:[extensionFilter]
};

@swagger:ServiceInfo { 
    title: "PizzaShackAPI",
    description: "This is a RESTFul API for Pizza Shack online pizza delivery store.
",
    serviceVersion: "1.0.0",
    contact: {name: "John Doe", email: "architecture@pizzashack.com", url: "http://www.pizzashack.com"},
    license: {name: "Apache 2.0", url: "http://www.apache.org/licenses/LICENSE-2.0.html"}
}

@http:ServiceConfig {
    basePath: "/pizzashack/1.0.0",
    authConfig:{
        authProviders:["oauth2"],
        authentication:{enabled:true}
    },
    cors: {
            allowOrigins: ["*"],
            allowCredentials: false,
            allowHeaders: ["authorization","Access-Control-Allow-Origin","Content-Type","SOAPAction"]
        }
}

@gateway:API {
    name:"PizzaShackAPI",
    apiVersion: "1.0.0" 
}
service<http:Service> PizzaShackAPI_1_0_0 bind apiListener {

    @swagger:ResourceInfo {
        summary: "",
        description: "Create a new Order",
        parameters: [
            {
                name: "body",
                inInfo: "body",
                description: "Order object that needs to be added", 
                required: true, 
                allowEmptyValue: ""
            }
        ]
    }
    @http:ResourceConfig {
        methods:["POST"],
        path:"/order",
        authConfig:{
            authProviders:["oauth2"],
            authentication:{enabled:true}
        }
    }
    @gateway:RateLimit{policy : "Unlimited"}
    postOrder (endpoint outboundEp, http:Request req) {
    string urlPostfix = untaint req.rawPath.replace("/pizzashack/1.0.0","");
    var clientResponse = PizzaShackAPI_1_0_0_EP->forward(urlPostfix, req);

        match clientResponse {

            http:Response res => {
                outboundEp->respond(res) but { error e =>
                log:printError("Error when sending response", err = e) };
            }

            error err => {
                http:Response res = new;
                res.statusCode = 500;
                res.setPayload(err.message);
                outboundEp->respond(res) but { error e =>
                log:printError("Error when sending response", err = e) };
            }
        }
    }

    @swagger:ResourceInfo {
        summary: "",
        description: "Return a list of available menu items"
    }
    @http:ResourceConfig {
        methods:["GET"],
        path:"/menu",
        authConfig:{
            authProviders:["oauth2"],
            authentication:{enabled:true}
        }
    }
    @gateway:RateLimit{policy : "Unlimited"}
    getMenu (endpoint outboundEp, http:Request req) {
    string urlPostfix = untaint req.rawPath.replace("/pizzashack/1.0.0","");
    var clientResponse = PizzaShackAPI_1_0_0_EP->forward(urlPostfix, req);

        match clientResponse {

            http:Response res => {
                outboundEp->respond(res) but { error e =>
                log:printError("Error when sending response", err = e) };
            }

            error err => {
                http:Response res = new;
                res.statusCode = 500;
                res.setPayload(err.message);
                outboundEp->respond(res) but { error e =>
                log:printError("Error when sending response", err = e) };
            }
        }
    }

    @swagger:ResourceInfo {
        summary: "",
        description: "Get details of an Order",
        parameters: [
            {
                name: "orderId",
                inInfo: "path",
                description: "Order Id", 
                required: true, 
                allowEmptyValue: ""
            }
        ]
    }
    @http:ResourceConfig {
        methods:["GET"],
        path:"/order/{orderId}",
        authConfig:{
            authProviders:["oauth2"],
            authentication:{enabled:true}
        }
    }
    @gateway:RateLimit{policy : "Unlimited"}
    getOrder__orderId_ (endpoint outboundEp, http:Request req) {
    string urlPostfix = untaint req.rawPath.replace("/pizzashack/1.0.0","");
    var clientResponse = PizzaShackAPI_1_0_0_EP->forward(urlPostfix, req);

        match clientResponse {

            http:Response res => {
                outboundEp->respond(res) but { error e =>
                log:printError("Error when sending response", err = e) };
            }

            error err => {
                http:Response res = new;
                res.statusCode = 500;
                res.setPayload(err.message);
                outboundEp->respond(res) but { error e =>
                log:printError("Error when sending response", err = e) };
            }
        }
    }

    @swagger:ResourceInfo {
        summary: "",
        description: "Update an existing Order",
        parameters: [
            {
                name: "orderId",
                inInfo: "path",
                description: "Order Id", 
                required: true, 
                allowEmptyValue: ""
            },
            {
                name: "body",
                inInfo: "body",
                description: "Order object that needs to be added", 
                required: true, 
                allowEmptyValue: ""
            }
        ]
    }
    @http:ResourceConfig {
        methods:["PUT"],
        path:"/order/{orderId}",
        authConfig:{
            authProviders:["oauth2"],
            authentication:{enabled:true}
        }
    }
    @gateway:RateLimit{policy : "Unlimited"}
    putOrder__orderId_ (endpoint outboundEp, http:Request req) {
    string urlPostfix = untaint req.rawPath.replace("/pizzashack/1.0.0","");
    var clientResponse = PizzaShackAPI_1_0_0_EP->forward(urlPostfix, req);

        match clientResponse {

            http:Response res => {
                outboundEp->respond(res) but { error e =>
                log:printError("Error when sending response", err = e) };
            }

            error err => {
                http:Response res = new;
                res.statusCode = 500;
                res.setPayload(err.message);
                outboundEp->respond(res) but { error e =>
                log:printError("Error when sending response", err = e) };
            }
        }
    }

    @swagger:ResourceInfo {
        summary: "",
        description: "Delete an existing Order",
        parameters: [
            {
                name: "orderId",
                inInfo: "path",
                description: "Order Id", 
                required: true, 
                allowEmptyValue: ""
            }
        ]
    }
    @http:ResourceConfig {
        methods:["DELETE"],
        path:"/order/{orderId}",
        authConfig:{
            authProviders:["oauth2"],
            authentication:{enabled:true}
        }
    }
    @gateway:RateLimit{policy : "Unlimited"}
    deleteOrder__orderId_ (endpoint outboundEp, http:Request req) {
    string urlPostfix = untaint req.rawPath.replace("/pizzashack/1.0.0","");
    var clientResponse = PizzaShackAPI_1_0_0_EP->forward(urlPostfix, req);

        match clientResponse {

            http:Response res => {
                outboundEp->respond(res) but { error e =>
                log:printError("Error when sending response", err = e) };
            }

            error err => {
                http:Response res = new;
                res.statusCode = 500;
                res.setPayload(err.message);
                outboundEp->respond(res) but { error e =>
                log:printError("Error when sending response", err = e) };
            }
        }
    }

}

@Description {value:"Representation of the Subscription filter"}
@Field {value:"filterRequest: request filter method which attempts to validate the subscriptions"}
public type ExtensionFilter object {

    @Description {value:"filterRequest: Request filter function"}
    public function filterRequest (http:Listener listener, http:Request request, http:FilterContext context) returns
                                                                                                                 boolean {
        return true;
    }

    public function filterResponse(http:Response response, http:FilterContext context) returns boolean {
        match <boolean> context.attributes[gateway:FILTER_FAILED] {
            boolean failed => {
                if (failed) {
                    int statusCode = check <int>context.attributes[gateway:HTTP_STATUS_CODE];
                    if(statusCode == gateway:UNAUTHORIZED) {
                        setAuthenticationErrorResponse(response, context );
                    } else if (statusCode ==  gateway:FORBIDDEN) {
                        setAuthorizationErrorResponse(response, context );
                    } else if (statusCode ==  gateway:THROTTLED_OUT){
                        setThrottleFailureResponse(response, context );
                    } else {
                        setGenericErrorResponse(response, context );
                    }

                    return true;
                    //return gateway:createFilterResult(false, statusCode, errorMessage);
                }
            } error err => {
            //Nothing to handle
            return true;
        }
        }

        return true;
    }

};

@Description {value:"This method can be used to send custom error message in an authentication failute"}
function setAuthenticationErrorResponse(http:Response response, http:FilterContext context) {
    //Un comment the following code and set the proper error messages

    //int statusCode = check <int>context.attributes[gateway:HTTP_STATUS_CODE];
    //string errorDescription = <string>context.attributes[gateway:ERROR_DESCRIPTION];
    //string errorMesssage = <string>context.attributes[gateway:ERROR_MESSAGE];
    //int errorCode = check <int>context.attributes[gateway:ERROR_CODE];
    //response.statusCode = statusCode;
    //response.setContentType(gateway:APPLICATION_JSON);
    //json payload = {fault : {
    //    code : errorCode,
    //    message : errorMesssage,
    //    description : errorDescription
    //}};
    //response.setJsonPayload(payload);
}

@Description {value:"This method can be used to send custom error message in an authorization failute"}
function setAuthorizationErrorResponse(http:Response response, http:FilterContext context) {

}

@Description {value:"This method can be used to send custom error message when message throttled out"}
function setThrottleFailureResponse(http:Response response, http:FilterContext context) {

}

@Description {value:"This method can be used to send custom general error message "}
function setGenericErrorResponse(http:Response response, http:FilterContext context) {

}

