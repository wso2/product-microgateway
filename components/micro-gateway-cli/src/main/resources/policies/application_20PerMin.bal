import ballerina/io;
import ballerina/runtime;
import ballerina/http;
import ballerina/log;
import wso2/gateway;

stream<gateway:IntermediateStream> s20PerMinintermediateStream = new;
stream<gateway:GlobalThrottleStreamDTO> s20PerMinresultStream = new;
stream<gateway:EligibilityStreamDTO> s20PerMineligibilityStream = new;
stream<gateway:RequestStreamDTO> s20PerMinreqCopy= gateway:requestStream;
stream<gateway:GlobalThrottleStreamDTO> s20PerMinglobalThrotCopy = gateway:globalThrottleStream;

function initApplication20PerMinPolicy() {

    forever {
        from s20PerMinreqCopy
        select s20PerMinreqCopy.messageID as messageID, (s20PerMinreqCopy.appTier == "20PerMin") as isEligible, s20PerMinreqCopy.appKey as throttleKey, 0 as expiryTimestamp
        => (gateway:EligibilityStreamDTO[] counts) {

            foreach var c in counts{

                s20PerMineligibilityStream.publish(c);
            }
        }


        from s20PerMineligibilityStream
        window gateway:timeBatch(60000,0)
        where s20PerMineligibilityStream.isEligible == true
        select s20PerMineligibilityStream.throttleKey as throttleKey, count() as eventCount, true as stopOnQuota, s20PerMineligibilityStream.expiryTimestamp as expiryTimeStamp
        group by s20PerMineligibilityStream.throttleKey
        => (gateway:IntermediateStream[] counts) {

            foreach var c in counts{

                s20PerMinintermediateStream.publish(c);
            }
        }

        from s20PerMinintermediateStream
        select s20PerMinintermediateStream.throttleKey, getThrottleValues20PerMin(s20PerMinintermediateStream.eventCount) as isThrottled, s20PerMinintermediateStream.stopOnQuota, s20PerMinintermediateStream.expiryTimeStamp
        group by s20PerMineligibilityStream.throttleKey
        => (gateway:GlobalThrottleStreamDTO[] counts) {
            foreach var c in counts{

                s20PerMinresultStream.publish(c);
            }
        }

        from s20PerMinresultStream
        window gateway:emitOnStateChange(s20PerMinresultStream.throttleKey, s20PerMinresultStream.isThrottled, "s20PerMinresultStream")
        select s20PerMinresultStream.throttleKey as throttleKey, s20PerMinresultStream.isThrottled, s20PerMinresultStream.stopOnQuota, s20PerMinresultStream.expiryTimeStamp
        => (gateway:GlobalThrottleStreamDTO[] counts) {
            foreach var c in counts{

                s20PerMinglobalThrotCopy.publish(c);

            }
        }

    }
}

function getThrottleValues20PerMin(int eventCount) returns boolean{
    if(eventCount>= 20){
        return true;
    }else{
        return false;
    }
}