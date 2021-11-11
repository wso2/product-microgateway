'use strict';

var utils = require('../utils/writer.js');
var Response = require('../service/ResponseService');

module.exports.handleResponse = function handleResponse (req, res, next, body) {
  Response.handleResponse(body)
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
