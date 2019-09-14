import ballerina/http;

public function validateRequest (http:Caller caller, http:Request req) {
   json newPayload = { Intercept: {
                        RequestCode: "e123",
                        message: "Successfully intercepted",
                        description: "Description"
                    } };
  req.setJsonPayload(<@untainted> newPayload);
}