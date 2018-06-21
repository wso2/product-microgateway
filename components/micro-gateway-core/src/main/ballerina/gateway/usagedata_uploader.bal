import ballerina/http;
import ballerina/log;
import ballerina/mime;

stream<string> filesToUpload;


function multipartSender(string file) returns http:Response {
    endpoint http:Client clientEP {
        url: uploadingUrl
    };
    mime:Entity filePart = new;
    filePart.setContentDisposition(getContentDispositionForFormData("file"));
    filePart.setFileAsEntityBody(file);
    mime:Entity[] bodyParts = [filePart];
    http:Request request = new;

    request.addHeader(AUTH_HEADER, getBasicAuthHeaderValue("admin", "admin"));
    request.addHeader(FILE_NAME, file);
    request.addHeader(ACCEPT, APPLICATION_JSON);
    request.setBodyParts(bodyParts);
    var returnResponse = clientEP->post("", request);

    match returnResponse {
        error err => {
            http:Response response = new;
            string errorMessage = "Error occurred while sending multipart request: SC " + 500;
            response.setPayload(errorMessage);
            response.statusCode = 500;
            log:printError(errorMessage, err = err);
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