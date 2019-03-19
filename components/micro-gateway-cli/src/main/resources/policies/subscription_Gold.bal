import ballerina/io;
import ballerina/runtime;
import ballerina/http;
import ballerina/log;
import wso2/gateway;

stream<gateway:IntermediateStream> sGoldintermediateStream = new;
stream<gateway:GlobalThrottleStreamDTO> sGoldresultStream = new;
stream<gateway:EligibilityStreamDTO> sGoldeligibilityStream = new;
stream<gateway:RequestStreamDTO> sGoldreqCopy= gateway:requestStream;
stream<gateway:GlobalThrottleStreamDTO> sGoldglobalThrotCopy = gateway:globalThrottleStream;

function initSubscriptionGoldPolicy() {

    forever {
        from sGoldreqCopy
        select sGoldreqCopy.messageID as messageID, (sGoldreqCopy.subscriptionTier == "Gold") as isEligible, sGoldreqCopy.subscriptionKey as throttleKey, 0 as expiryTimestamp
        => (gateway:EligibilityStreamDTO[] counts) {

            foreach var c in counts{

                sGoldeligibilityStream.publish(c);
            }
        }


        from sGoldeligibilityStream
        window gateway:timeBatch(60000,0)
        where sGoldeligibilityStream.isEligible == true
        select sGoldeligibilityStream.throttleKey as throttleKey, count() as eventCount, true as stopOnQuota, sGoldeligibilityStream.expiryTimestamp as expiryTimeStamp
        group by sGoldeligibilityStream.throttleKey
        => (gateway:IntermediateStream[] counts) {

            foreach var c in counts{

                sGoldintermediateStream.publish(c);
            }
        }

        from sGoldintermediateStream
        select sGoldintermediateStream.throttleKey, getThrottleValuesGold(sGoldintermediateStream.eventCount) as isThrottled, sGoldintermediateStream.stopOnQuota, sGoldintermediateStream.expiryTimeStamp
        group by sGoldeligibilityStream.throttleKey
        => (gateway:GlobalThrottleStreamDTO[] counts) {
            foreach var c in counts{

                sGoldresultStream.publish(c);
            }
        }

        from sGoldresultStream
        window gateway:emitOnStateChange(sGoldresultStream.throttleKey, sGoldresultStream.isThrottled, "sGoldresultStream")
        select sGoldresultStream.throttleKey as throttleKey, sGoldresultStream.isThrottled, sGoldresultStream.stopOnQuota, sGoldresultStream.expiryTimeStamp
        => (gateway:GlobalThrottleStreamDTO[] counts) {
            foreach var c in counts{

                sGoldglobalThrotCopy.publish(c);

            }
        }

    }
}

function getThrottleValuesGold(int eventCount) returns boolean{
    if(eventCount>= 5000){
        return true;
    }else{
        return false;
    }
}