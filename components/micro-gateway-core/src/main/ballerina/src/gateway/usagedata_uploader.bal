// Copyright (c)  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
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
import ballerina/mime;

//stream<string> filesToUpload = new;


public function multipartSender(string location, string file, string username, string password) returns http:Response {
    mime:Entity filePart = new;
    filePart.setFileAsEntityBody(location + file);
    filePart.setContentDisposition(getContentDispositionForFormData(file));
    mime:Entity[] bodyParts = [filePart];
    http:Request request = new;

    request.addHeader(AUTH_HEADER, getBasicAuthHeaderValue(username, password));
    request.addHeader(FILE_NAME, file);
    request.addHeader(ACCEPT, APPLICATION_JSON);
    request.setBodyParts(bodyParts);
    var returnResponse = analyticsFileUploadEndpoint->post("", request);

        if(returnResponse is error) {
            http:Response response = new;
            string errorMessage = "Error occurred while sending multipart request: SC 500";
            response.setPayload(errorMessage);
            response.statusCode = 500;
            printFullError(KEY_UPLOAD_TASK, returnResponse);
            return response;
        }
        else  {
            return returnResponse;
        }

}


public function getContentDispositionForFormData(string partName)
             returns (mime:ContentDisposition) {
    mime:ContentDisposition contentDisposition = new;
    contentDisposition.name = "file";
    contentDisposition.fileName = partName;
    contentDisposition.disposition = "form-data";
    return contentDisposition;
}

public function getBasicAuthHeaderValue(string username, string password) returns string {
    string credentials = username + ":" + password;
    string encodedVal = encodeValueToBase64(credentials);
    return "Basic " + encodedVal;  
}