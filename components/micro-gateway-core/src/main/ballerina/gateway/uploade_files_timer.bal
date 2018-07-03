import ballerina/io;
import ballerina/internal;
import ballerina/task;
import ballerina/math;
import ballerina/runtime;
import ballerina/log;

string uploadingUrl;

future timerFtr = start timerTask();

function searchFilesToUpload() returns error? {
    int cnt = 0;
    string fileLocation = retrieveConfig(API_USAGE_PATH, API_USAGE_DIR);
    internal:Path ex = new(fileLocation);
    internal:Path[] pathList = check ex.list();
    foreach pathEntry in pathList {
        string fileName = pathEntry.getName();
        if (fileName.contains(ZIP_EXTENSION)) {
            http:Response response =  multipartSender(fileLocation + PATH_SEPERATOR,  pathEntry.getName());
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
    log:printDebug("File were not present to upload yet:" + e.message);
}

function timerTask() {
    task:Timer? timer;
    map vals = getConfigMapValue(ANALYTICS);
    int timeSpan =  check <int> vals[UPLOADING_TIME_SPAN];
    (function() returns error?) onTriggerFunction = searchFilesToUpload;
    function(error) onErrorFunction = informError;
    timer = new task:Timer(onTriggerFunction, onErrorFunction, timeSpan, delay = 5000);
    timer.start();
}

