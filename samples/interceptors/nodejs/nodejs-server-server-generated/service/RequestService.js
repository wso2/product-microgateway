'use strict';


/**
 * Handle Request
 *
 * body RequestHandlerRequestBody Content of the request
 (optional)
 * returns RequestHandlerResponseBody
 **/
exports.handleRequest = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "directRespond" : false,
  "headersToAdd" : {
    "content-type" : "application/xml",
    "content-length" : "40"
  },
  "interceptorContext" : {
    "key1" : "value1",
    "key2" : "value2"
  },
  "headersToRemove" : [ "key1", "key2" ],
  "trailersToAdd" : {
    "trailer1-key" : "value"
  },
  "dynamicEndpoint" : {
    "endpointName" : "my-dynamic-endpoint"
  },
  "body" : "PGhlbGxvPndvcmxkPC9oZWxsbz4K",
  "responseCode" : 200
};
    if (Object.keys(examples).length > 0) {
      resolve(examples[Object.keys(examples)[0]]);
    } else {
      resolve();
    }
  });
}

