import ballerina/io;
import ballerina/runtime;
import ballerina/http;
import ballerina/log;
import wso2/gateway;

function initApplication20PerMinPolicy() {
    stream<gateway:IntermediateStream> intermediateStream = new;
    stream<gateway:GlobalThrottleStreamDTO> resultStream = new;
    stream<gateway:EligibilityStreamDTO> eligibilityStream = new;
    forever {
        from gateway:requestStream
        select gateway:requestStream.messageID, (gateway:requestStream.appTier == "20PerMin") as isEligible, gateway:requestStream.appKey as throttleKey
        => (gateway:EligibilityStreamDTO[] counts) {
            foreach var c in counts{
                eligibilityStream.publish(c);
            }
        }

        from eligibilityStream
        gateway:timeBatch([60000, 0])
        where eligibilityStream.isEligible == true
        select eligibilityStream.throttleKey, count() as eventCount, true as stopOnQuota, 0 as expiryTimeStamp
        group by eligibilityStream.throttleKey
        => (gateway:IntermediateStream[] counts) {
            foreach var c in counts{
                intermediateStream.publish(c);
            }
        }

        from intermediateStream
        select intermediateStream.throttleKey, getThrottleValue20(intermediateStream.eventCount) as isThrottled, intermediateStream.stopOnQuota, intermediateStream.expiryTimeStamp
        group by eligibilityStream.throttleKey
        => (gateway:GlobalThrottleStreamDTO[] counts) {
            foreach var c in counts{
                resultStream.publish(c);
            }
        }

        from resultStream
        gateway:emitOnStateChange([resultStream.throttleKey, resultStream.isThrottled])
        select resultStream.throttleKey, resultStream.isThrottled, resultStream.stopOnQuota, resultStream.expiryTimeStamp
        => (gateway:GlobalThrottleStreamDTO[] counts) {
            foreach var c in counts{
                gateway:globalThrottleStream.publish(c);
            }
        }
    }
}

function getThrottleValue20(int eventCount) returns boolean{
    if(eventCount>=20){
        return true;
    }else{
        return false;
    }
}
