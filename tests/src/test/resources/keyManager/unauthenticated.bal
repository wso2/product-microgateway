
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
import ballerina/io;

endpoint http:Listener keyManagerListener {
    port:9443
};

@http:ServiceConfig {
    basePath:"/services/APIKeyValidationService"
}
service<http:Service> keyManagerService bind keyManagerListener{

    @http:ResourceConfig {
        methods:["POST"],
        path:"/"
    }
    validateKey (endpoint caller, http:Request req) {
        http:Response resp = new;
        string payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap
        .org/soap/envelope/\"><soapenv:Body><ns:validateKeyResponse xmlns:ns=\"http://org.apache.axis2/xsd\"><ns:return
xmlns:ax2137=\"http://dto.impl.apimgt.carbon.wso2.org/xsd\" xmlns:ax2129=\"http://keymgt.apimgt.carbon.wso2.org/xsd\"
xmlns:ax2131=\"http://api.apimgt.carbon.wso2.org/xsd\" xmlns:ax2133=\"http://model.api.apimgt.carbon.wso2.org/xsd\"
xmlns:ax2134=\"http://dto.api.apimgt.carbon.wso2.org/xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"
xsi:type=\"ax2137:APIKeyValidationInfoDTO\"><ax2137:apiName>PizzaShackAPI</ax2137:apiName><ax2137:apiPublisher>admin
</ax2137:apiPublisher><ax2137:authorized>false</ax2137:authorized><ax2137:validationStatus>900901</ax2137:validationStatus></ns:return></ns:validateKeyResponse></soapenv:Body></soapenv:Envelope>";
        resp.setTextPayload(payload, contentType = "text/xml");
        _ = caller->respond(resp);
    }
}
