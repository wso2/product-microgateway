local json = require "home.wso2.interceptor.lib.json"
-- json library: https://github.com/rxi/json.lua
local base64 = require'home.wso2.interceptor.lib.base64'
-- base64 library: https://github.com/iskolbin/lbase64

local interceptor = {}

function interceptor.handle_request_interceptor(request_handle, cluster_name, resource_path, timeout)
    -- TODO: (renuka) check what to log as trace and debug
    -- read headers and the payload
    local request_headers = request_handle:headers()
    local request_id = request_headers:get("x-request-id")
    local request_headers_table = {}
    for key, value in pairs(request_headers) do
        request_headers_table[key] = value
    end
    local request_body = request_handle:body():getBytes(0, request_handle:body():length())

    -- build the interceptor request body
    local interceptor_request_body = {
        headers=request_headers_table,
        body=base64.encode(request_body)
    }

    local _, interceptor_response_body_str = request_handle:httpCall(
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

    local interceptor_response_body = json.decode(interceptor_response_body_str)
    -- if "body" is not defined or null (i.e. {} or {"body": null}) do not update the body
    if interceptor_response_body.body ~= nil then
        request_handle:logDebug("Updating body for the request_id: " .. request_id)
        local content_length = request_handle:body():setBytes(base64.decode(interceptor_response_body.body))
        request_handle:headers():replace("content-length", content_length)
    end

    -- priority: headersToRemove, headersToReplace, headersToAdd
    if interceptor_response_body.headersToAdd ~= nil then
        for key, val in pairs(interceptor_response_body.headersToAdd) do
            request_handle:headers():add(key, val)
        end
    end
    if interceptor_response_body.headersToReplace ~= nil then
        for key, val in pairs(interceptor_response_body.headersToReplace) do
            request_handle:headers():replace(key, val)
        end
    end
    if interceptor_response_body.headersToRemove ~= nil then
        for _, key in ipairs(interceptor_response_body.headersToRemove) do
            request_handle:headers():remove(key)
        end
    end
end

return interceptor
