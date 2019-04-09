import ballerina/io;
import ballerina/runtime;
import ballerina/http;
import ballerina/log;
import wso2/gateway;

stream<gateway:IntermediateStream> s20PerMinintermediateStream = new;
stream<gateway:GlobalThrottleStreamDTO> s20PerMinresultStream = new;
stream<gateway:EligibilityStreamDTO> s20PerMineligibilityStream = new;
stream<gateway:RequestStreamDTO> s20PerMinreqCopy = gateway:requestStream;
stream<gateway:GlobalThrottleStreamDTO> s20PerMinglobalThrotCopy = gateway:globalThrottleStream;

function initApplication20PerMinPolicy() {

    forever {
        from s20PerMinreqCopy
        select s20PerMinreqCopy.messageID as messageID, (s20PerMinreqCopy.appTier == "20PerMin") as
        isEligible, s20PerMinreqCopy.appKey as throttleKey, 0 as expiryTimestamp
        => (gateway:EligibilityStreamDTO[] counts) {
            foreach var c in counts {
                s20PerMineligibilityStream.publish(c);
            }
        }

        from s20PerMineligibilityStream
        throttler:timeBatch(60000)
        where s20PerMineligibilityStream.isEligible == true
        select s20PerMineligibilityStream.throttleKey as throttleKey, count() as eventCount, true as
        stopOnQuota, expiryTimeStamp
        group by s20PerMineligibilityStream.throttleKey
        => (gateway:IntermediateStream[] counts) {
            foreach var c in counts {
                s20PerMinintermediateStream.publish(c);
            }
        }

        from s20PerMinintermediateStream
        select s20PerMinintermediateStream.throttleKey, s20PerMinintermediateStream.eventCount >= 20 as isThrottled,
        s20PerMinintermediateStream.stopOnQuota, s20PerMinintermediateStream.expiryTimeStamp
        group by s20PerMineligibilityStream.throttleKey
        => (gateway:GlobalThrottleStreamDTO[] counts) {
            foreach var c in counts {
                s20PerMinresultStream.publish(c);
            }
        }

        from s20PerMinresultStream
        throttler:emitOnStateChange(s20PerMinresultStream.throttleKey, s20PerMinresultStream.isThrottled)
        select s20PerMinresultStream.throttleKey as throttleKey, s20PerMinresultStream.isThrottled,
        s20PerMinresultStream.stopOnQuota, s20PerMinresultStream.expiryTimeStamp
        => (gateway:GlobalThrottleStreamDTO[] counts) {
            foreach var c in counts {
                s20PerMinglobalThrotCopy.publish(c);
            }
        }
    }
}

