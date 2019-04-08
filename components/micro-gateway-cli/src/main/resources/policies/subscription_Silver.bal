import ballerina/io;
import ballerina/runtime;
import ballerina/http;
import ballerina/log;
import wso2/gateway;

stream<gateway:IntermediateStream> sSilverintermediateStream = new;
stream<gateway:GlobalThrottleStreamDTO> sSilverresultStream = new;
stream<gateway:EligibilityStreamDTO> sSilvereligibilityStream = new;
stream<gateway:RequestStreamDTO> sSilverreqCopy = gateway:requestStream;
stream<gateway:GlobalThrottleStreamDTO> sSilverglobalThrotCopy = gateway:globalThrottleStream;

function initSubscriptionSilverPolicy() {

    forever {
        from sSilverreqCopy
        select sSilverreqCopy.messageID as messageID, (sSilverreqCopy.subscriptionTier == "Silver") as
        isEligible, sSilverreqCopy.subscriptionKey as throttleKey, 0 as expiryTimestamp
        => (gateway:EligibilityStreamDTO[] counts) {
            foreach var c in counts {
                sSilvereligibilityStream.publish(c);
            }
        }

        from sSilvereligibilityStream
        throttler:timeBatch(60000)
        where sSilvereligibilityStream.isEligible == true
        select sSilvereligibilityStream.throttleKey as throttleKey, count() as eventCount, true as
        stopOnQuota, expiryTimeStamp
        group by sSilvereligibilityStream.throttleKey
        => (gateway:IntermediateStream[] counts) {
            foreach var c in counts {
                sSilverintermediateStream.publish(c);
            }
        }

        from sSilverintermediateStream
        select sSilverintermediateStream.throttleKey, sSilverintermediateStream.eventCount >= 2000 as isThrottled,
        sSilverintermediateStream.stopOnQuota, sSilverintermediateStream.expiryTimeStamp
        group by sSilvereligibilityStream.throttleKey
        => (gateway:GlobalThrottleStreamDTO[] counts) {
            foreach var c in counts {
                sSilverresultStream.publish(c);
            }
        }

        from sSilverresultStream
        throttler:emitOnStateChange(sSilverresultStream.throttleKey, sSilverresultStream.isThrottled)
        select sSilverresultStream.throttleKey as throttleKey, sSilverresultStream.isThrottled,
        sSilverresultStream.stopOnQuota, sSilverresultStream.expiryTimeStamp
        => (gateway:GlobalThrottleStreamDTO[] counts) {
            foreach var c in counts {
                sSilverglobalThrotCopy.publish(c);
            }
        }
    }
}

