import ballerina/io;
import ballerina/runtime;
import ballerina/http;
import ballerina/log;
import wso2/gateway;

stream<gateway:IntermediateStream> s10PerMinintermediateStream = new;
stream<gateway:GlobalThrottleStreamDTO> s10PerMinresultStream = new;
stream<gateway:EligibilityStreamDTO> s10PerMineligibilityStream = new;
stream<gateway:RequestStreamDTO> s10PerMinreqCopy= gateway:requestStream;
stream<gateway:GlobalThrottleStreamDTO> s10PerMinglobalThrotCopy = gateway:globalThrottleStream;

function initApplication10PerMinPolicy() {

    forever {
        from s10PerMinreqCopy
        select s10PerMinreqCopy.messageID as messageID, (s10PerMinreqCopy.appTier == "10PerMin") as
        isEligible, s10PerMinreqCopy.appKey as throttleKey, 0 as expiryTimestamp
        => (gateway:EligibilityStreamDTO[] counts) {
            foreach var c in counts {
                s10PerMineligibilityStream.publish(c);
            }
        }

        from s10PerMineligibilityStream
        throttler:timeBatch(60000)
        where s10PerMineligibilityStream.isEligible == true
        select s10PerMineligibilityStream.throttleKey as throttleKey, count() as eventCount, true as
        stopOnQuota, expiryTimeStamp
        group by s10PerMineligibilityStream.throttleKey
        => (gateway:IntermediateStream[] counts) {
            foreach var c in counts{
                s10PerMinintermediateStream.publish(c);
            }
        }

        from s10PerMinintermediateStream
        select s10PerMinintermediateStream.throttleKey, s10PerMinintermediateStream.eventCount>= 10 as isThrottled,
        s10PerMinintermediateStream.stopOnQuota, s10PerMinintermediateStream.expiryTimeStamp
        group by s10PerMineligibilityStream.throttleKey
        => (gateway:GlobalThrottleStreamDTO[] counts) {
            foreach var c in counts{
                s10PerMinresultStream.publish(c);
            }
        }

        from s10PerMinresultStream
        throttler:emitOnStateChange(s10PerMinresultStream.throttleKey, s10PerMinresultStream.isThrottled)
        select s10PerMinresultStream.throttleKey as throttleKey, s10PerMinresultStream.isThrottled,
        s10PerMinresultStream.stopOnQuota, s10PerMinresultStream.expiryTimeStamp
        => (gateway:GlobalThrottleStreamDTO[] counts) {
            foreach var c in counts {
                s10PerMinglobalThrotCopy.publish(c);
            }
        }
    }
}

