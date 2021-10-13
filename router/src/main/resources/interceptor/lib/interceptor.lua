local json = require 'home.wso2.interceptor.lib.json'
-- json library: https://github.com/rxi/json.lua
local base64 = require 'home.wso2.interceptor.lib.base64'
-- base64 library: https://github.com/iskolbin/lbase64

local interceptor = {}
-- TODO: (renuka) check what to log as trace and debug

local function send_http_call(handle, interceptor_request_body, cluster_name, resource_path, timeout)
    local err, interceptor_response_body_str = handle:httpCall(
        cluster_name,
        {
            [":method"] = "POST",
            [":path"] = resource_path,
            [":authority"] = "cc-interceptor",
            ["content-type"] = "application/json",
            ["accept"] = "application/json",
        },
        json.encode(interceptor_request_body),
        timeout,
        false -- async false, wait for response from interceptor service
    )
    return err, interceptor_response_body_str
end

local function modify_headers(interceptor_response_body, handle)
    -- priority: headersToRemove, headersToReplace, headersToAdd
    if interceptor_response_body.headersToAdd ~= nil then
        for key, val in pairs(interceptor_response_body.headersToAdd) do
            handle:headers():add(key, val)
        end
    end
    if interceptor_response_body.headersToReplace ~= nil then
        for key, val in pairs(interceptor_response_body.headersToReplace) do
            handle:headers():replace(key, val)
        end
    end
    if interceptor_response_body.headersToRemove ~= nil then
        for _, key in ipairs(interceptor_response_body.headersToRemove) do
            handle:headers():remove(key)
        end
    end
end

local function modify_body(interceptor_response_body, request_handle, request_id)
    -- if "body" is not defined or null (i.e. {} or {"body": null}) do not update the body
    if interceptor_response_body.body ~= nil then
        request_handle:logDebug("Updating body for the request_id: " .. request_id)
        local content_length = request_handle:body():setBytes(base64.decode(interceptor_response_body.body))
        request_handle:headers():replace("content-length", content_length)
    end
end

local shared_request_headers
local shared_request_body

function interceptor.handle_request_interceptor(request_handle, cluster_name, resource_path, timeout)
    -- read headers and the payload
    local request_headers = request_handle:headers()
    local request_id = request_headers:get("x-request-id")
    local request_headers_table = {}
    for key, value in pairs(request_headers) do
        request_headers_table[key] = value
    end
    local request_body = request_handle:body():getBytes(0, request_handle:body():length())

    shared_request_headers = request_headers_table
    shared_request_body = base64.encode(request_body)

    -- build the interceptor request body
    local interceptor_request_body = {
        requestHeaders = shared_request_headers,
        requestBody = shared_request_body
    }

    -- TODO: (renuka) handle errors
    local _, interceptor_response_body_str = send_http_call(request_handle, interceptor_request_body, cluster_name, resource_path, timeout)
    local interceptor_response_body = json.decode(interceptor_response_body_str)

    modify_body(interceptor_response_body, request_handle, request_id)
    modify_headers(interceptor_response_body, request_handle)
end

function interceptor.handle_response_interceptor(response_handle, cluster_name, resource_path, timeout)
    -- read headers and the payload
    local request_headers = response_handle:headers()
    local request_id = request_headers:get("x-request-id")
    local request_headers_table = {}
    for key, value in pairs(request_headers) do
        request_headers_table[key] = value
    end
    local request_body = response_handle:body():getBytes(0, response_handle:body():length())

    -- build the interceptor request body
    local interceptor_request_body = {
        requestHeaders = shared_request_headers,
        requestBody = shared_request_body,
        responseHeaders = request_headers_table,
        responseBody = base64.encode(request_body)
    }

    -- TODO: (renuka) handle errors
    local _, interceptor_response_body_str = send_http_call(response_handle, interceptor_request_body, cluster_name, resource_path, timeout)
    local interceptor_response_body = json.decode(interceptor_response_body_str)

    modify_body(interceptor_response_body, response_handle, request_id)
    modify_headers(interceptor_response_body, response_handle)
end

return interceptor
