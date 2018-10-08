import ballerina/io;
import ballerina/runtime;
import ballerina/http;
import ballerina/log;
import wso2/gateway;

function initSubscriptionSilverPolicy() {
    stream<gateway:GlobalThrottleStreamDTO> resultStream;
    stream<gateway:EligibilityStreamDTO> eligibilityStream;
    forever {
        from gateway:requestStream
        select messageID, (subscriptionTier == "Silver") as isEligible, subscriptionKey as throttleKey
        => (gateway:EligibilityStreamDTO[] counts) {
            eligibilityStream.publish(counts);
        }

        from eligibilityStream
        throttler:timeBatch(60000, 0)
        where isEligible == true
        select throttleKey, count(messageID) >= 2000 as isThrottled, true as stopOnQuota, expiryTimeStamp
        group by throttleKey
        => (gateway:GlobalThrottleStreamDTO[] counts) {
            resultStream.publish(counts);
        }

        from resultStream
        throttler:emitOnStateChange(throttleKey, isThrottled)
        select throttleKey, isThrottled, stopOnQuota, expiryTimeStamp
        => (gateway:GlobalThrottleStreamDTO[] counts) {
            gateway:globalThrottleStream.publish(counts);
        }
    }
}