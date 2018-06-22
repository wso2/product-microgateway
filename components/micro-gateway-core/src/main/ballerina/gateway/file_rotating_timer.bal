import ballerina/io;
import ballerina/internal;
import ballerina/task;
import ballerina/math;
import ballerina/runtime;
import ballerina/log;


future rotatingFtr = start rotatingTask();

function sendFileRotatingEvent() returns error? {
    int cnt = 0;
    internal:Path path = new(API_USAGE_FILE);

    if (path.exists()) {
        io:println("file exists");
        var result = rotateFile(API_USAGE_FILE);
        match result {
            string name => {
                log:printInfo("File rotated successfully.");
            }
            error err => {
                log:printError("Error occurred while rotating the file: ", err = err);
            }
        }
        return ();
    } else {
        error er = {message: "No files present to rotate."};
        return er;
    }
    //foreach pathEntry in pathList {
    //    string fileName = pathEntry.getName();
    //    if (fileName.contains(ZIP_EXTENSION)) {
    //        http:Response response =  multipartSender(pathEntry.getName());
    //        if (response.statusCode == 201) {
    //            var result = pathEntry.delete();
    //        } else {
    //            log:printError("Error occurred while uploading file");
    //        }
    //        cnt++;
    //    }
    //}
    //if ( cnt == 0 ) {
    //    error er = {message: "No files present to upload."};
    //    return er;
    //} else {
    //    return ();
    //}
}

function errorOnRotating(error e) {
    log:printInfo("File were not present to rotate:" + e.message);
}

function rotatingTask() {
    task:Timer? rotatinTimer;
    map vals = getConfigMapValue(ANALYTICS);
    int timeSpan =  check <int> vals[ROTATING_TIME];
    (function() returns error?) onTriggerFunction = sendFileRotatingEvent;
    function(error) onErrorFunction = errorOnRotating;
    rotatinTimer = new task:Timer(onTriggerFunction, onErrorFunction, timeSpan, delay = timeSpan + 5000);
    rotatinTimer.start();
}

