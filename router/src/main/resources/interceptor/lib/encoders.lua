--[[
Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

]]

local json = require 'home.wso2.interceptor.lib.json'
-- json library: https://github.com/rxi/json.lua
local base64 = require 'home.wso2.interceptor.lib.base64'
-- base64 library: https://github.com/iskolbin/lbase64

---decode string with given encoder
---@param encoded_string any
---@param handle any
---@param request_id any
---@param is_request_flow any
---@return string - decoded string
---@return boolean - true if error returned
local function decode_string(decode_func, decode_func_desc, encoded_string, handle, shared_info, request_id, is_request_flow)
    local status, decoded = pcall(decode_func, encoded_string)
    if status then
        return decoded, false
    end

    log_interceptor_service_error(
            handle,
            request_id,
            is_request_flow,
            'Invalid ' .. decode_func_desc .. ' encoded body from interceptor service, reason: "' .. decoded .. '"'
    )
    respond_error(handle, shared_info, {
        error_message = "Internal Server Error",
        error_description = "Internal Server Error",
        error_code = ERROR_CODES.DECODE_ERROR
    }, is_request_flow)
    return "", true
end

function base64_encode(str)
    return base64.encode(str)
end

function base64_decode(encoded_string, handle, shared_info, request_id, is_request_flow)
    return decode_string(base64.decode, "base64", encoded_string, handle, shared_info, request_id, is_request_flow)
end

function json_encode(str)
    return json.encode(str)
end

function json_decode(encoded_string, handle, shared_info, request_id, is_request_flow)
    return decode_string(json.decode, "json", encoded_string, handle, shared_info, request_id, is_request_flow)
end