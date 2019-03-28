import ballerina/io;
import ballerina/runtime;
import ballerina/http;
import ballerina/log;
import wso2/gateway;

stream<gateway:IntermediateStream> sUnauthenticatedintermediateStream = new;
stream<gateway:GlobalThrottleStreamDTO> sUnauthenticatedresultStream = new;
stream<gateway:EligibilityStreamDTO> sUnauthenticatedeligibilityStream = new;
stream<gateway:RequestStreamDTO> sUnauthenticatedreqCopy = gateway:requestStream;
stream<gateway:GlobalThrottleStreamDTO> sUnauthenticatedglobalThrotCopy = gateway:globalThrottleStream;

function initSubscriptionUnauthenticatedPolicy() {

    forever {
        from sUnauthenticatedreqCopy
        select sUnauthenticatedreqCopy.messageID as messageID, (sUnauthenticatedreqCopy.subscriptionTier ==
        "Unauthenticated") as isEligible, sUnauthenticatedreqCopy.subscriptionKey as throttleKey, 0 as expiryTimestamp
        => (gateway:EligibilityStreamDTO[] counts) {
            foreach var c in counts {
                sUnauthenticatedeligibilityStream.publish(c);
            }
        }


        from sUnauthenticatedeligibilityStream
        window gateway:timeBatch(60000)
        where sUnauthenticatedeligibilityStream.isEligible == true
        select sUnauthenticatedeligibilityStream.throttleKey as throttleKey, count() as eventCount, true as stopOnQuota,
        sUnauthenticatedeligibilityStream.expiryTimestamp as expiryTimeStamp
        group by sUnauthenticatedeligibilityStream.throttleKey
        => (gateway:IntermediateStream[] counts) {
            foreach var c in counts {
                sUnauthenticatedintermediateStream.publish(c);
            }
        }

        from sUnauthenticatedintermediateStream
        select sUnauthenticatedintermediateStream.throttleKey, getThrottleValuesUnauthenticated(
                                                                   sUnauthenticatedintermediateStream.eventCount) as
        isThrottled, sUnauthenticatedintermediateStream.stopOnQuota, sUnauthenticatedintermediateStream.expiryTimeStamp
        group by sUnauthenticatedeligibilityStream.throttleKey
        => (gateway:GlobalThrottleStreamDTO[] counts) {
            foreach var c in counts {
                sUnauthenticatedresultStream.publish(c);
            }
        }

        from sUnauthenticatedresultStream
        window gateway:emitOnStateChange(sUnauthenticatedresultStream.throttleKey, sUnauthenticatedresultStream.
            isThrottled, "sUnauthenticatedresultStream")
        select sUnauthenticatedresultStream.throttleKey as throttleKey, sUnauthenticatedresultStream.isThrottled,
        sUnauthenticatedresultStream.stopOnQuota, sUnauthenticatedresultStream.expiryTimeStamp
        => (gateway:GlobalThrottleStreamDTO[] counts) {
            foreach var c in counts {
                sUnauthenticatedglobalThrotCopy.publish(c);
            }
        }

    }
}

function getThrottleValuesUnauthenticated(int eventCount) returns boolean {
    if (eventCount >= 500) {
        return true;
    } else {
        return false;
    }
}