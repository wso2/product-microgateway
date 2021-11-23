'use strict';

var utils = require('../utils/writer.js');
var Request = require('../service/RequestService');

module.exports.handleRequest = function handleRequest (req, res, next, body) {
  Request.handleRequest(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
