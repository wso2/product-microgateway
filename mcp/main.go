/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package main

import (
	"context"
	"fmt"
	"net/http"

	"github.com/gin-gonic/gin"

	mcp "mcp-server/pkg/mcp"
	"mcp-server/pkg/service"
)

var logger = service.GetLogger()

func serveRequest(c *gin.Context) {
	var mcpRequest mcp.MCPRequest

	if err := c.BindJSON(&mcpRequest); err != nil {
		logger.Error("Failed to bind JSON", "error", err)
		return
	}
	// Validate the request
	if mcpRequest.ToolName == "" {
		logger.Error("Tool name is required")
		c.JSON(http.StatusBadRequest, mcp.HandleBadRequest("Tool name is required"))
		return
	} else if mcpRequest.Arguments == "" {
		logger.Error("Arguments are required")
		c.JSON(http.StatusBadRequest, mcp.HandleBadRequest("Arguments are required"))
		return
	} else if mcpRequest.Schema == "" {
		logger.Warn("Input schema is not provided")
	}
	if mcpRequest.IsProxy {
		if mcpRequest.API.APIName == "" {
			logger.Warn("API name is not proided")
		} else if mcpRequest.API.Endpoint == "" {
			logger.Error("API endpoint is required")
			c.JSON(http.StatusBadRequest, mcp.HandleBadRequest("API endpoint is required"))
			return
		} else if mcpRequest.API.Context == "" {
			logger.Error("API context is required")
			c.JSON(http.StatusBadRequest, mcp.HandleBadRequest("API context is required"))
			return
		} else if mcpRequest.API.Version == "" {
			logger.Error("API version is required")
			c.JSON(http.StatusBadRequest, mcp.HandleBadRequest("API version is required"))
			return
		} else if mcpRequest.API.Path == "" {
			logger.Error("Resource path is required")
			c.JSON(http.StatusBadRequest, mcp.HandleBadRequest("Resource path is required"))
			return
		} else if mcpRequest.API.Verb == "" {
			logger.Error("HTTP verb is required")
			c.JSON(http.StatusBadRequest, mcp.HandleBadRequest("HTTP verb is required"))
			return
		}
	} else {
		if mcpRequest.Backend.Endpoint == "" {
			logger.Error("Backend endpoint is required")
			c.JSON(http.StatusBadRequest, mcp.HandleBadRequest("Backend endpoint is required"))
			return
		} else if mcpRequest.Backend.Target == "" {
			logger.Error("Backend target is required")
			c.JSON(http.StatusBadRequest, mcp.HandleBadRequest("Backend target is required"))
			return
		} else if mcpRequest.Backend.Verb == "" {
			logger.Error("Backend verb is required")
			c.JSON(http.StatusBadRequest, mcp.HandleBadRequest("Backend verb is required"))
			return
		}
	}

	// Set logging context
	ctx := context.Background()

	ctx = context.WithValue(ctx, service.ToolNameKey, mcpRequest.ToolName)
	if mcpRequest.API.APIName != "" {
		ctx = context.WithValue(ctx, service.ApiNameKey, mcpRequest.API.APIName)
	}

	if mcpRequest.IsProxy && mcpRequest.API.Auth == "" {
		logger.WarnContext(ctx, "Authentication is not provided for the underlying API. Assuming no authentication is required.")
	}

	// Call the underlying API
	cfg := service.GetConfig()
	if cfg.Log.Debug {
		logger.Debug("Calling underlying API/Service", "toolName", mcpRequest.ToolName)
	}
	resp, code, err := mcp.CallUnderlyingAPI(ctx, &mcpRequest)
	if err != nil {
		logger.ErrorContext(ctx, "Failed to call underlying API", "error", err)
		result := mcp.Result{
			Code:     code,
			Response: err.Error(),
		}
		c.SecureJSON(code, result)
		return
	}
	// Wrap the actual response in a result object and return 200 OK
	result := mcp.Result{
		Code:     code,
		Response: resp,
	}
	c.SecureJSON(200, result)
}

func main() {
	router := service.GetRouter()
	router.POST("/mcp", serveRequest)
	cfg, err := service.InitConfig()
	if err != nil {
		logger.Error("Failed to get configurations", "error", err)
		return
	}
	address := fmt.Sprintf("%s:%d", cfg.Server.Host, cfg.Server.Port)
	logger.Info(fmt.Sprintf("Starting server on %s...", address))
	if cfg.Server.Secure {
		err = router.RunTLS(address, cfg.Server.CertPath, cfg.Server.KeyPath)
	} else {
		logger.Warn("Starting server in insecure mode.")
		err = router.Run(address)
	}
	if err != nil {
		logger.Error("Failed to start the service", "error", err)
		return
	}

}
