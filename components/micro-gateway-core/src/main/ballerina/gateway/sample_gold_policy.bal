import ballerina/io;
import ballerina/runtime;
import ballerina/http;
import ballerina/log;

future goldPolicyFtr = start initGoldPolicy();

function initGoldPolicy() {
    stream<GlobalThrottleStreamDTO> resultStream;
    stream<EligibilityStreamDTO> eligibilityStream;
    runtime:sleep(1000);
    forever {
        from requestStream
        select messageID, (subscriptionTier == "Bronze") as isEligible, subscriptionKey as throttleKey
        => (EligibilityStreamDTO[] counts) {
            eligibilityStream.publish(counts);
        }

        from eligibilityStream
        throttler:timeBatch(60000)
        where isEligible == true
        select throttleKey, count(messageID) >= 5 as isThrottled, 13234 as expiryTimeStamp
        group by throttleKey
        => (GlobalThrottleStreamDTO[] counts) {
            resultStream.publish(counts);
        }

        from resultStream
        throttler:emitOnStateChange(throttleKey, isThrottled)
        select throttleKey, isThrottled, expiryTimeStamp
        => (GlobalThrottleStreamDTO[] counts) {
            globalThrottleStream.publish(counts);
        }
    }
}