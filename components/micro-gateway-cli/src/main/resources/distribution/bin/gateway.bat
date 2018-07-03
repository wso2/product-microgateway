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

REM ----------------------------------------------------------------------------
REM Startup Script for Gateway Server
REM
REM Environment Variable Prerequisites
REM
REM   BALLERINA_HOME      Home of Ballerina installation.
REM
REM NOTE: Borrowed generously from Apache Tomcat startup scripts.
REM -----------------------------------------------------------------------------
setlocal EnableDelayedExpansion

if ""%1%""==""-v"" ( set verbose=T ) else ( set verbose=F )
if %verbose%==T ( echo Verbose mode enabled )

REM Get the location of this(gateway.bat) file
SET PRGDIR=%~dp0
SET GWHOME=%PRGDIR%..
REM  set BALLERINA_HOME
set BALLERINA_HOME=%GWHOME%\runtime
if %verbose%==T echo BALLERINA_HOME environment variable is set to %BALLERINA_HOME%
if %verbose%==T echo GWHOME environment variable is set to %GWHOME%

rem Slurp the command line arguments. This loop allows for an unlimited number
rem of arguments (up to the command line limit, anyway).
:setupArgs
	if %verbose%==T echo [%date% %time%] DEBUG: Processing argument : `%1`
	if ""%1""=="""" goto :callBallerina

	if ""%1""==""--debug""    goto commandDebug
	if ""%1""==""-debug""   goto commandDebug
	if ""%1""==""debug""  goto commandDebug
	shift
goto setupArgs

:commandDebug
	if %verbose%==T echo [%date% %time%] DEBUG: Running commandDebug

	shift
	set DEBUG_PORT=%1
	if "%DEBUG_PORT%"=="" goto noDebugPort
	echo Please start the remote debugging client to continue...
goto :callBallerina

:noDebugPort
	echo Please specify the debug port after the ballerina debug option
goto end


:callBallerina
	rem ---------- Run balx with ballerina ----------------
    set "separator=/"
    set log_path="%GWHOME%\conf\micro-gw.conf"
    call set unix_style_path=%%log_path:\=%separator%%%
	if %verbose%==T echo [%date% %time%] DEBUG: balx location = "%GWHOME%\exec\pro2.balx" 
	if %verbose%==T echo [%date% %time%] DEBUG: b7a.http.accesslog.path = "%GWHOME%\logs\access_logs"
	if %verbose%==T echo [%date% %time%] DEBUG: configs = %unix_style_path%
	if %verbose%==T echo [%date% %time%] DEBUG: Starting micro gateway server...

    ballerina run "%GWHOME%\exec\pro2.balx" -e b7a.http.accesslog.path=%unix_style_path% --config "%GWHOME%\conf\micro-gw.conf" "%*"
:end
goto endlocal

:endlocal

:END
