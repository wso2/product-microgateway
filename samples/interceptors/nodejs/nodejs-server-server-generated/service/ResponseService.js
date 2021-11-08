'use strict';


/**
 * Handle Response
 *
 * body ResponseHandlerRequestBody Content of the request
 (optional)
 * returns ResponseHandlerResponseBody
 **/
exports.handleResponse = function(body) {
  return new Promise(function(resolve, reject) {
    var examples = {};
    examples['application/json'] = {
  "headersToAdd" : {
    "content-type" : "application/xml",
    "content-length" : "40"
  },
  "headersToRemove" : [ "key1", "key2" ],
  "trailersToAdd" : {
    "trailer1-key" : "value"
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

