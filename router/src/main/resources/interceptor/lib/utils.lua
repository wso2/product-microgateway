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

--- utils module
-- @module utils
local utils = {}

function is_starts_with(text, startsWith)
    return string.sub(text, 1, string.len(startsWith)) == startsWith
end

function is_valid_header(key)
    local key_lower = string.lower(key)
    return not is_starts_with(key_lower, "x-wso2") and not is_starts_with(key_lower, "x-envoy")
end

function direct_respond(handle, headers, body, shared_info)
    shared_info[RESPONSE.DIRECT_RESPOND] = true
    handle:streamInfo():dynamicMetadata():set(LUA_FILTER_NAME, SHARED_INFO_META_KEY, shared_info)
    handle:respond(
            headers,
            body
    )
end

--- log error related to interceptor service
---@param handle table
---@param request_id string
---@param is_request_flow boolean
---@param message string
function log_interceptor_service_error(handle, request_id, is_request_flow, message)
    local intercept_path = is_request_flow and "request" or "response"
    handle:logErr('Invalid ' .. intercept_path .. ' interceptor service response, message: "' .. message .. '"' .. ', request_id: "' .. request_id .. '"')
end

--- respond error to the client
---@param handle table
---@param shared_info table
---@param error_info {error_message: string, error_description: string, error_code: string}
---@param is_request_flow boolean
function respond_error(handle, shared_info, error_info, is_request_flow)
    local resp_body = '{"error_message": "' .. error_info.error_message .. '", "error_description": "' .. error_info.error_description ..
            '", "code": "' .. error_info.error_code .. '"}'

    --#region request flow
    if is_request_flow then
        direct_respond(
                handle,
                {[STATUS] = "500"},
                resp_body,
                shared_info
        )
        return
    end
    --#endregion

    --#region response flow
    local backend_headers = handle:headers()
    local headers_to_remove = {}
    for key, _ in pairs(backend_headers) do
        headers_to_remove[key] = "" -- can not remove headers while iterating, hence adding first
    end
    for key, _ in pairs(headers_to_remove) do -- remove all headers from backend
        backend_headers:remove(key)
    end
    backend_headers:add(STATUS, "500")

    local content_length = handle:body(true):setBytes(resp_body)
    handle:headers():replace("content-length", content_length)
    return
    --#endregion
end

function table.shallow_copy(src, des)
    for k,v in pairs(src) do
        des[k] = v
    end
end

--- log body retrieved from the handle
---@param handle table
---@param log_message string
function utils.wire_log_body(handle, log_message, log_body_enabled)
    if log_body_enabled then 
        local log_output = "\n" .. "[wirelog]" .. log_message .. "\n"
        if handle:body() then
            handle:logInfo(log_output .. handle:body():getBytes(0, handle:body():length()) .. log_output)
        else 
            handle:logInfo("Body is empty")
        end
    end
end

--- log headers retrieved from the handle
---@param handle table
---@param log_message string
function utils.wire_log_headers(handle, log_message, log_headers_enabled)
    if log_headers_enabled then 
        local headers = handle:headers()
        local log_output = "\n"
        if headers ~= nil then
            for header_name, header_value in pairs(headers) do
                log_output = log_output .. "[wirelog]" .. log_message .. header_name .. ": " .. header_value .. "\n"
            end
        end
        handle:logInfo(log_output)
    end
end

--- log trailers retrieved from the handle
---@param handle table
---@param log_message string
function utils.wire_log_trailers(handle, log_message, log_trailers_enabled)
    if log_trailers_enabled then 
        local trailers = handle:trailers()
        local log_output = "\n"
        if trailers ~= nil then
            for trailer_name, trailer_value in pairs(trailers) do
                log_output = log_output .. "[wirelog]" .. log_message .. trailer_name .. ": " .. trailer_value .. "\n"
            end
        end
        handle:logInfo(log_output)
    end
end

--- log body and headers retrieved from the handle
---@param log_message_body string
---@param log_message_header string
function utils.wire_log(handle, log_message_body, log_message_header, log_message_trailer, wire_log_config)
    utils.wire_log_headers(handle, log_message_header, wire_log_config.log_headers_enabled)
    utils.wire_log_body(handle, log_message_body, wire_log_config.log_body_enabled)
    utils.wire_log_trailers(handle, log_message_trailer, wire_log_config.log_trailers_enabled)
end

return utils
