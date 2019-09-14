import ballerina/http;

public function interceptPerAPIRequest (http:Caller caller, http:Request req) {
 json newPayload = { APIIntercept: {
                        RequestCode: "e123",
                        message: "Successfully intercepted",
                        description: "Description"
                    } };
  req.setJsonPayload(<@untainted> newPayload);
}