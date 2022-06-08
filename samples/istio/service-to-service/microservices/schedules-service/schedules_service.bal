import ballerina/log;
import ballerina/http;

listener http:Listener ep0 = new (8081);

service /'schedules\-service/v1 on ep0 {
    isolated resource function get schedules(string? 'from, string? to, string? startTime, string? endTime) returns ScheduleItemInfo[]|http:InternalServerError {
        lock {
            ScheduleItem[] filteredScheduleItems = [];
            foreach ScheduleItem t in schedules {
                if t?.trainId != () {
                    ScheduleItemInfo|error scheduleInfo = getScheduleInfo(t);
                    if scheduleInfo is ScheduleItemInfo {
                        filteredScheduleItems.push(scheduleInfo);
                    } else {
                        log:printError("Error retriving train info.", scheduleInfo);
                        return <http:InternalServerError>{body: {message: "Internal Server Error"}};
                    }
                }
            }
            return filteredScheduleItems.clone();
        }
    }

    resource function post schedules(@http:Payload {} ScheduleItem payload) returns http:Ok {
        lock {
            payload.trainId = nextIndex.toString();
        }
        lock {
            schedules.push(payload.clone());
        }
        return {body: {message: "ScheduleItem added successfully"}};
    }

    resource function get schedules/[int id]() returns ScheduleItemInfo|http:NotFound|http:InternalServerError {
        if !isScheduleExists(id) {
            return <http:NotFound>{body: {message: "ScheduleItem Not Found"}};
        } else {
            lock {
                ScheduleItemInfo|error scheduleInfo = getScheduleInfo(schedules[id - 1]);
                if scheduleInfo is ScheduleItemInfo {
                    return scheduleInfo.clone();
                } else {
                    log:printError("Error retriving train info.", scheduleInfo);
                    return <http:InternalServerError>{body: {message: "Internal Server Error"}};
                }
            }
        }
    }

    resource function put schedules/[int id](@http:Payload {} ScheduleItem payload) returns http:Ok|http:NotFound {
        if !isScheduleExists(id) {
            return <http:NotFound>{body: {message: "ScheduleItem Not Found"}};
        } else {
            payload.trainId = id.toString();
            lock {
                schedules[id - 1] = payload.clone();
            }
            return <http:Ok>{body: {message: "ScheduleItem updated successfully"}};
        }
    }

    resource function delete schedules/[int id]() returns http:Ok|http:NotFound {
        if !isScheduleExists(id) {
            return <http:NotFound>{body: {message: "ScheduleItem Not Found"}};
        } else {
            lock {
                schedules[id - 1] = {};
            }
            return <http:Ok>{body: {status: "ScheduleItem deleted successfully"}};
        }
    }
}
