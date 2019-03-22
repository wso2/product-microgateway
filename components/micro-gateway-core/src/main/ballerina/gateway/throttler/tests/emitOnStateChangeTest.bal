import ballerina/http;
import ballerina/log;
import ballerina/io;
import ballerina/test;
import ballerina/config;
import ballerina/runtime;

type InputDTO record {
    string throttleKey;
    boolean isThrottled;
};

type OutputDTO record{
    string throttleKey;
    boolean isThrottled;
    string outp;
};

int indexx = 0;
stream<InputDTO> inputStreamTest1 = new;
stream<OutputDTO > outputStreamTest1 = new;
OutputDTO[] globalArray = [];

@test:Config
public function emittest(){

    log:printInfo("starting emit on state change test");
    OutputDTO[] arrayT = startEmitTest();
    json[] testOut = [{throttleKey:"123", isThrottled:false, outp:"testout"}, {throttleKey:"456", isThrottled:false, outp:"testout"}, {throttleKey:"123", isThrottled:true, outp:"testout"}, {throttleKey:"123", isThrottled:true, outp:"testout"}];
    test:assertEquals(arrayT, testOut, msg = "Test failed");
}

function startEmitTest() returns (OutputDTO[]) {

    InputDTO t1 = { throttleKey: "123", isThrottled: false };
    InputDTO t2 = { throttleKey: "456", isThrottled: false };
    InputDTO t3 = { throttleKey: "123", isThrottled: false };
    InputDTO t4 = { throttleKey: "123", isThrottled: true };
    InputDTO t5 = { throttleKey: "456", isThrottled: false };
    InputDTO t6 = { throttleKey: "123", isThrottled: true };

    testwindow();

    outputStreamTest1.subscribe(function(OutputDTO e) {printT(e);});

    inputStreamTest1.publish(t1);
    inputStreamTest1.publish(t2);
    runtime:sleep(1200);
    inputStreamTest1.publish(t3);
    inputStreamTest1.publish(t4);
    runtime:sleep(3000);
    inputStreamTest1.publish(t5);
    inputStreamTest1.publish(t6);
    runtime:sleep(1200);


    return globalArray;
}

function testwindow() {

    forever {
        from inputStreamTest1

        window emitOnStateChange(inputStreamTest1.throttleKey, inputStreamTest1.isThrottled,"inputStreamTest1")

        select inputStreamTest1.throttleKey, inputStreamTest1.isThrottled, "testout" as outp

        => (OutputDTO[] emp) {

            foreach var e in emp {
                outputStreamTest1.publish(e);
            }
        }

    }
}

function printT(OutputDTO e) {
    addToGlobalArray(e);
}

function addToGlobalArray(OutputDTO e) {
    globalArray[indexx] = e;
    indexx = indexx + 1;
}


