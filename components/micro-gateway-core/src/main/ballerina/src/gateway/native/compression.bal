// Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerinax/java;
# Represent all compression related errors.
#
# + message - The error message
public type CompressionError record {|
    string message;
|};

# Compresses a directory.
#
# + dirPath - Path of the directory to be compressed
# + destDir - Path of the directory to place the compressed file
# + return - An error if an error occurs during the compression process
public function compress(string dirPath, string destDir) returns error? {
    handle sourceDir = java:fromString(dirPath);
    handle destZipDir = java:fromString(destDir);
    return jCompress(sourceDir, destZipDir);
}



function jCompress(handle sourceDir, handle destZipDir) returns error? = @java:Method {
    name: "compress",
    class: "org.wso2.micro.gateway.core.compression.Compress"
} external;


