import ballerina/io;
import ballerina/runtime;
import ballerina/http;
import ballerina/log;
import wso2/gateway;

function initSubscriptionUnauthenticatedPolicy() {
    stream<gateway:GlobalThrottleStreamDTO> resultStream=new;
    stream<gateway:EligibilityStreamDTO> eligibilityStream=new;
    forever {
        from gateway:requestStream
        select gateway:requestStream.messageID, (gateway:requestStream.subscriptionTier == "Unauthenticated") as isEligible, gateway:requestStream.subscriptionKey as throttleKey
        => (gateway:EligibilityStreamDTO[] counts) {
        foreach var c in counts {
            eligibilityStream.publish(c);
        }
        }

        from eligibilityStream
        window gateway:timeBatch(60000, 0)
        where eligibilityStream.isEligible == true
        select eligibilityStream.throttleKey, count() as eventCount, true as stopOnQuota, 0 as expiryTimeStamp
        group by eligibilityStream.throttleKey
        => (IntermediateStream[] counts) {
            foreach var c in counts{
                intermediateStream.publish(c);
            }
        }

        from intermediateStream
        select intermediateStream.throttleKey, getThrottleValue500(intermediateStream.eventCount) as isThrottled, intermediateStream.stopOnQuota, intermediateStream.expiryTimeStamp
        group by eligibilityStream.throttleKey
        => (gateway:GlobalThrottleStreamDTO[] counts) {
            foreach var c in counts{
                resultStream.publish(c);
            }
        }

        from resultStream
        window gateway:emitOnStateChange(resultStream.throttleKey, resultStream.isThrottled)
        select resultStream.throttleKey, resultStream.isThrottled, resultStream.stopOnQuota, resultStream.expiryTimeStamp
        => (gateway:GlobalThrottleStreamDTO[] counts) {
            foreach var c in counts{
                gateway:globalThrottleStream.publish(c);
            }
        }
    }
}

function getThrottleValue500(int eventCount) returns boolean{
    if(eventCount>=500){
        return true;
    }else{
        return false;
    }
}