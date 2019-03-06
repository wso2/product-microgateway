import ballerina/io;
import ballerina/runtime;
import ballerina/http;
import ballerina/log;
import wso2/gateway;
import wso2/throttler;

function initSubscriptionSilverPolicy() {
    stream<gateway:GlobalThrottleStreamDTO> resultStream;
    stream<gateway:EligibilityStreamDTO> eligibilityStream;
    forever {
        from gateway:requestStream
        select gateway:requestStream.messageID, (gateway:requestStream.subscriptionTier == "Silver") as isEligible, gateway:requestStream.subscriptionKey as throttleKey
        => (gateway:EligibilityStreamDTO[] counts) {
            eligibilityStream.publish(counts);
        }

        from eligibilityStream
        throttler:timeBatch([60000, 0])
        where eligibilityStream.isEligible == true
        select eligibilityStream.throttleKey, eligibilityStream.messageID.length() >= 2000 as isThrottled, true as stopOnQuota, 0 as expiryTimeStamp
        group by eligibilityStream.throttleKey
        => (gateway:GlobalThrottleStreamDTO[] counts) {
            resultStream.publish(counts);
        }

        from resultStream
        throttler:emitOnStateChange(resultStream.throttleKey, resultStream.isThrottled)
        select resultStream.throttleKey, resultStream.isThrottled, resultStream.stopOnQuota, resultStream.expiryTimeStamp
        => (gateway:GlobalThrottleStreamDTO[] counts) {
            gateway:globalThrottleStream.publish(counts);
        }
    }
}