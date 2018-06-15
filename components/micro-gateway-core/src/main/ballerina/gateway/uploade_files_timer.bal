import ballerina/io;
import ballerina/internal;
import ballerina/task;
import ballerina/math;
import ballerina/runtime;


int count;
task:Timer? timer;


function searchFilesToUpload() returns error? {
    io:println("task starting");
    int cnt = 0;
    internal:Path ex = new("");
    internal:Path[] ar = check ex.list();
    io:println(ar);
    foreach pathEntry in ar {
        io:println("starting the path entries");
        string fileName = pathEntry.getName();
        io:println(fileName);
        if ( fileName.contains("zip")) {
            http:Response response =  multipartSender(pathEntry.getName());
            if (response.statusCode == 201) {
                var result = pathEntry.delete();
            } else {
                io:println("File uploading failed");
            }
            cnt++;
        }
    }
    if ( cnt == 0 ) {
        io:println("No files to upload");
        error er = {message: "No files present to upload."};
        return er;
    } else {
         io:println("Files were uploaded");
         return ();
    }
}


function timerTask() {
    io:println("Timer task demo");
    (function() returns error?) onTriggerFunction = searchFilesToUpload;

    function(error) onErrorFunction = cleanup;

    timer = new task:Timer(onTriggerFunction, onErrorFunction,
    3000, delay = 2000);

    timer.start();

}


function cleanup(error e) {
    count = count + 1;
    io:println("Cleaning up...");
    io:println(count);
}


//future timerFtr = start timerTask();