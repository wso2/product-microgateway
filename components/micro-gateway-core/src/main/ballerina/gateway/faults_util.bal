// Copyright (c)  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.




public function getFaultMetaData(FaultDTO dto) returns string {
    return dto.clientType;
}

public function getFaultPayloadData(FaultDTO dto) returns string {
    return dto.consumerKey + OBJ + dto.context + OBJ + dto.apiVersion + OBJ + dto.apiName + OBJ +
        dto.resourcePath + OBJ + dto.method + OBJ + dto.versionOnly + OBJ + dto.errorCode + OBJ +
        dto.errorMessage + OBJ + dto.faultTime + OBJ + dto.userName + OBJ + dto.tenantDomain
        + OBJ + dto.hostName + OBJ + dto.apiPublisher + OBJ + dto.applicationName + OBJ +
        dto.applicationId + OBJ + dto.protocol;

}

public function getEventFromFaultData(FaultDTO dto) returns EventDTO {
    EventDTO eventDTO;
    eventDTO.streamId = "org.wso2.apimgt.statistics.fault:1.0.0";
    eventDTO.timeStamp = getCurrentTime();
    eventDTO.metaData = getFaultMetaData(dto);
    eventDTO.correlationData = "null";
    eventDTO.payloadData = getFaultPayloadData(dto);
    return eventDTO;
}