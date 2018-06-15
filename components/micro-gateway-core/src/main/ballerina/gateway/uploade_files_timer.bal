import ballerina/io;
import ballerina/internal;
import ballerina/task;
import ballerina/math;
import ballerina/runtime;
import ballerina/log;

task:Timer? timer;

future timerFtr = start timerTask();

function searchFilesToUpload() returns error? {
    int cnt = 0;
    internal:Path ex = new("");
    internal:Path[] pathList = check ex.list();
    foreach pathEntry in pathList {
        string fileName = pathEntry.getName();
        if ( fileName.contains("zip")) {
            http:Response response =  multipartSender(pathEntry.getName());
            if (response.statusCode == 201) {
                var result = pathEntry.delete();
            } else {
                log:printError("Error occurred while uploading file");
            }
            cnt++;
        }
    }
    if ( cnt == 0 ) {
        error er = {message: "No files present to upload."};
        return er;
    } else {
        return ();
    }
}




function informError(error e) {
    log:printInfo("File were not present to upload yet:" + e.message);
}

function timerTask() {
    (function() returns error?) onTriggerFunction = searchFilesToUpload;
    function(error) onErrorFunction = informError;
    timer = new task:Timer(onTriggerFunction, onErrorFunction, 60000, delay = 1000);
    timer.start();
}

