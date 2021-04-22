/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 *
 */

package err

const (
	// NotFoundCode resource not found error code
	NotFoundCode = 404
	// NotFoundMessage resource not found error message
	NotFoundMessage = "Not Found"
	// NotFoundDescription resource not found error description
	NotFoundDescription = "The requested resource is not available."

	// UaexCode enforcer connection failed error code
	UaexCode = 102500
	// UaexMessage enforcer connection failed error message
	UaexMessage = "Unclassified Validation Failure"
	// UaexDecription enforcer connection failed error description
	UaexDecription = "Error during validating the request"

	// UfCode upstream connection failed error code
	UfCode = 102503
	// UfMessage upstream connection failed error message
	UfMessage = "Upstream connection failed"

	// UtCode upstream connection timeout error code
	UtCode = 102504
	// UtMessage upstream connection timeout error message
	UtMessage = "Upstream connection timeout"

	// UoCode upstream overflow (circuit breaker) error code
	UoCode = 102505
	// UoMessage upstream overflow (circuit breaker) error message
	UoMessage = "Upstream overflow"

	// UrxCode upstream maximum connect attempts reached error code
	UrxCode = 102506
	// UrxMessage upstream maximum connect attempts reached error message
	UrxMessage = "Upstream maximum connect attempts reached"

	// NcCode upstream not configured for the resource error code
	NcCode = 102507
	// NcMessage upstream not configured for the resource error message
	NcMessage = "Upstream not configured for the resource"

	// UhCode no healthy upstream error code
	UhCode = 102508
	// UhMessage no healthy upstream error message
	UhMessage = "No healthy upstream"

	// UrCode upstream connection reset by the remote error code
	UrCode = 102509
	// UrMessage upstream connection reset by the remote error message
	UrMessage = "Upstream connection reset by the remote"

	// UcCode upstream connection termination error code
	UcCode = 102510
	// UcMessage upstream connection termination error message
	UcMessage = "Upstream connection termination"

	// LrCode connection reset by the gateway reached error code
	LrCode = 102511
	// LrMessage connection reset by the gateway error message
	LrMessage = "Connection reset by the gateway"

	// IhCode validation failure in strictly checked header error code
	IhCode = 102512
	// IhMessage validation failure in strictly checked header error message
	IhMessage = "Strictly checked header validation failure"

	// SiCode stream idle timeout error code
	SiCode = 102513
	// SiMessage stream idle timeout error message
	SiMessage = "Stream idle timeout"

	// DpeCode http protocol error in downstream request error code
	DpeCode = 102514
	// DpeMessage http protocol error in downstream request error message
	DpeMessage = "HTTP protocol error in downstream request"

	// UpeCode upstream http protocol error in upstream request error code
	UpeCode = 102515
	// UpeMessage http protocol error in upstream request error message
	UpeMessage = "HTTP protocol error in upstream request"

	// UmsdrCode upstream request reached max stream duration error code
	UmsdrCode = 102516
	// UmsdrMessage upstream request reached max stream duration error message
	UmsdrMessage = "Upstream request reached max stream duration"
)
