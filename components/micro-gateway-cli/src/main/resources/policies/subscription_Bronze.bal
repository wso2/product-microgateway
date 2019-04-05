import ballerina/io;
import ballerina/runtime;
import ballerina/http;
import ballerina/log;
import wso2/gateway;

stream<gateway:IntermediateStream> sBronzeintermediateStream = new;
stream<gateway:GlobalThrottleStreamDTO> sBronzeresultStream = new;
stream<gateway:EligibilityStreamDTO> sBronzeeligibilityStream = new;
stream<gateway:RequestStreamDTO> sBronzereqCopy= gateway:requestStream;
stream<gateway:GlobalThrottleStreamDTO> sBronzeglobalThrotCopy = gateway:globalThrottleStream;

function initSubscriptionBronzePolicy() {

    forever {
        from sBronzereqCopy
        select sBronzereqCopy.messageID as messageID, (sBronzereqCopy.subscriptionTier == "Bronze") as
        isEligible, sBronzereqCopy.subscriptionKey as throttleKey, 0 as expiryTimestamp
        => (gateway:EligibilityStreamDTO[] counts) {
            foreach var c in counts{
                sBronzeeligibilityStream.publish(c);
            }
        }

        from sBronzeeligibilityStream
        throttler:timeBatch(60000)
        where sBronzeeligibilityStream.isEligible == true
        select sBronzeeligibilityStream.throttleKey as throttleKey, count() as eventCount, true as
        stopOnQuota, expiryTimeStamp
        group by sBronzeeligibilityStream.throttleKey
        => (gateway:IntermediateStream[] counts) {
            foreach var c in counts{
                sBronzeintermediateStream.publish(c);
            }
        }

        from sBronzeintermediateStream
        select sBronzeintermediateStream.throttleKey, sBronzeintermediateStream.eventCount>= 1000 as isThrottled,
        sBronzeintermediateStream.stopOnQuota, sBronzeintermediateStream.expiryTimeStamp
        group by sBronzeeligibilityStream.throttleKey
        => (gateway:GlobalThrottleStreamDTO[] counts) {
            foreach var c in counts{
                sBronzeresultStream.publish(c);
            }
        }

        from sBronzeresultStream
        throttler:emitOnStateChange(sBronzeresultStream.throttleKey, sBronzeresultStream.isThrottled)
        select sBronzeresultStream.throttleKey as throttleKey, sBronzeresultStream.isThrottled,
        sBronzeresultStream.stopOnQuota, sBronzeresultStream.expiryTimeStamp
        => (gateway:GlobalThrottleStreamDTO[] counts) {
            foreach var c in counts{
                sBronzeglobalThrotCopy.publish(c);
            }
        }
    }
}

