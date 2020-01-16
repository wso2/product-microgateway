@echo off
REM ---------------------------------------------------------------------------
REM  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
REM
REM  Licensed under the Apache License, Version 2.0 (the "License");
REM  you may not use this file except in compliance with the License.
REM  You may obtain a copy of the License at
REM
REM  http://www.apache.org/licenses/LICENSE-2.0
REM
REM  Unless required by applicable law or agreed to in writing, software
REM  distributed under the License is distributed on an "AS IS" BASIS,
REM  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM  See the License for the specific language governing permissions and
REM  limitations under the License.

SET BALLERINA_HOME=%1
SET MAVEN_PROJECT_ROOT=%2
SET BAL_EXEC="%BALLERINA_HOME%\bin\ballerina"
SET GATEWAY_PROJECT="%MAVEN_PROJECT_ROOT%\src\main\ballerina\"

REM Quietly delete the directory structure in %GATEWAY_PROJECT%\target
RMDIR /S /Q %GATEWAY_PROJECT%\target

PUSHD %GATEWAY_PROJECT%
    %BAL_EXEC% build -c --experimental gateway
POPD

cp -r %GATEWAY_PROJECT%\target\caches\bir_cache %MAVEN_PROJECT_ROOT%\target
cp -r %GATEWAY_PROJECT%\target\caches\jar_cache %MAVEN_PROJECT_ROOT%\target
cp -r %GATEWAY_PROJECT%\target\balo %MAVEN_PROJECT_ROOT%\target

