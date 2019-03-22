import ballerina/http;
import ballerina/log;
import ballerina/io;
import ballerina/test;
import ballerina/config;
import ballerina/runtime;
import ballerina/time;

type InputStreamDTO record {
    string throttleKey;
    int expiryTimestamp?;
};

type OutputStreamDTO record{
    string throttleKey;
    int eventCount;
    int expiryTimeStamp=0;
};

int ind = 0;
stream<InputStreamDTO> inputStreamTimeBatchTest1 = new;
stream<OutputStreamDTO> outputStreamTimeBatchTest1 = new;
OutputStreamDTO[] gArray = [];

@test:Config
public function timebatchtest(){
    log:printInfo("starting time batch test");
    OutputStreamDTO[] arrayOut = startTimeBatchTest();

    test:assertEquals(arrayOut[1].eventCount, 2, msg = "Test time batch failed, batch count failed");
    test:assertEquals(arrayOut[5].eventCount, 4, msg = "Test time batch failed, batch count failed");
    test:assertFalse(arrayOut[5].expiryTimeStamp == 0, msg= "expiry time stamp is not added properly");
}


function startTimeBatchTest() returns (OutputStreamDTO[]) {

    InputStreamDTO t1 = { throttleKey: "123" , expiryTimestamp: 0};
    InputStreamDTO t2 = { throttleKey: "456" , expiryTimestamp: 0};
    InputStreamDTO t3 = { throttleKey: "625" , expiryTimestamp: 0};
    InputStreamDTO t4 = { throttleKey: "128" , expiryTimestamp: 0};
    InputStreamDTO t5 = { throttleKey: "856" , expiryTimestamp: 0};
    InputStreamDTO t6 = { throttleKey: "193" , expiryTimestamp: 0};

    testTimeBatchwindow();

    outputStreamTimeBatchTest1.subscribe(function(OutputStreamDTO e) {printTe(e);});

    inputStreamTimeBatchTest1.publish(t1);
    inputStreamTimeBatchTest1.publish(t2);
    runtime:sleep(2000);
    inputStreamTimeBatchTest1.publish(t3);
    inputStreamTimeBatchTest1.publish(t4);
    inputStreamTimeBatchTest1.publish(t5);
    inputStreamTimeBatchTest1.publish(t6);
    runtime:sleep(1000);



    return gArray;
}

function testTimeBatchwindow() {

    forever {
        from inputStreamTimeBatchTest1
        window timeBatch(2000,0)
        select inputStreamTimeBatchTest1.throttleKey as throttleKey, count() as eventCount, inputStreamTimeBatchTest1.expiryTimestamp as expiryTimeStamp
        => (OutputStreamDTO[] counts) {
            foreach var c in counts{
                outputStreamTimeBatchTest1.publish(c);
            }
        }

    }
}

function printTe(OutputStreamDTO e) {
    addToGArray(e);
}

function addToGArray(OutputStreamDTO e) {
    gArray[ind] = e;
    ind = ind + 1;
}


