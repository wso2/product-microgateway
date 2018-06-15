import ballerina/http;
import ballerina/log;
import ballerina/mime;

stream<string> filesToUpload;

endpoint http:Client clientEP {
    url: "https://localhost:9443"
};

function multipartSender(string file) returns http:Response {
    mime:Entity filePart = new;
    filePart.setContentDisposition(getContentDispositionForFormData("file"));
    filePart.setFileAsEntityBody(file);
    mime:Entity[] bodyParts = [filePart];
    http:Request request = new;

    request.addHeader("Authorization", getBasicAuthHeaderValue("admin", "admin"));
    request.addHeader("FileName", file);
    request.addHeader("Accept", "application/json");
    request.setBodyParts(bodyParts);
    io:println(request);
    var returnResponse = clientEP->post("/micro-gateway/v0.9/usage/upload-file",request);

    match returnResponse {
        error err => {
            http:Response response = new;
            string errorMessage = "Error occurred while sending multipart request: SC " + 500;
            response.setPayload(errorMessage);
            response.statusCode = 500;
            log:printError(errorMessage);
            return response;
        }
        http:Response returnResult => {
            log:printInfo("successfully uploaded the file: " + file);
            return returnResult;
        }
    }
}


function getContentDispositionForFormData(string partName)
             returns (mime:ContentDisposition) {
    mime:ContentDisposition contentDisposition = new;
    contentDisposition.name = partName;
    contentDisposition.disposition = "form-data";
    return contentDisposition;
}

function getBasicAuthHeaderValue(string username, string password) returns string {
    string credentials = username + ":" + password;
    match credentials.base64Encode() {
        string encodedVal => {
            return "Basic " + encodedVal;
        }
        error err => {
            throw err;
        }
    }
}