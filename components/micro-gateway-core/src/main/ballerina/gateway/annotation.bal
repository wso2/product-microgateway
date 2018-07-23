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

@Description {value:"Configuration used for resource level tier annotation"}
@Field {value:"policy: Rate limit speicified for the particular resource"}
public type TierConfiguration record {
    string policy;

};

@Description {value:"Resource level tier annotation"}
public annotation <resource> RateLimit TierConfiguration;

@Description {value:"Configuration used for api version annotation"}
@Field {value:"name: Name of the API"}
@Field {value:"apiVersion: version specified for the API"}
@Field {value:"publisher: provider of the API"}
@Field {value:"authorizationHeader: authorization header specified for the API"}
public type APIConfiguration record {
    string apiVersion;
    string name;
    string publisher;
    string authorizationHeader;

};

@Description {value:"API related details annotation"}
public annotation <service> API APIConfiguration;


