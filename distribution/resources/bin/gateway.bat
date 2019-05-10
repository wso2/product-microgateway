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

REM Set Java Xms and Xmx values. The values specified here will set in gateway runtime.
SET JAVA_XMS_VALUE="256m"
SET JAVA_XMX_VALUE="512m"

REM Get the location of this(gateway.bat) file
SET PRGDIR=%~sdp0
SET GWHOME=%PRGDIR%..
REM  set BALLERINA_HOME
set BALLERINA_HOME=%GWHOME%\runtime
if %verbose%==T echo BALLERINA_HOME environment variable is set to %BALLERINA_HOME%
if %verbose%==T echo GWHOME environment variable is set to %GWHOME%

REM Check if path to runtime executable is available
set last=""
for %%a in (%*) do set last=%%a
if "%last%"=="" set isInvalidPath=T
if not exist %last% set isInvalidPath=T
if "%isInvalidPath%"=="T" (
	echo Path to executable balx file is invalid
    goto end
)

REM Extract ballerina runtime
if not exist %GW_HOME%\runtime\ (
    call %PRGDIR%\tools.exe
    if ERRORLEVEL 0 (
        xcopy /y %GWHOME%\lib\gateway\*.jar %GWHOME%\runtime\bre\lib\ >nul
        xcopy /sy %GWHOME%\lib\gateway\balo\wso2 %GWHOME%\runtime\lib\repo\wso2\ >nul
    )
)

REM Slurp the command line arguments. This loop allows for an unlimited number
REM of arguments (up to the command line limit, anyway).
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
	REM ---------- Run balx with ballerina ----------------
	REM Change the windows style `\` path separator to unix style `/path/to/file` for log file path
	set "separator=/"
	set log_path="%GWHOME%\logs\access_logs"
	call set unix_style_path=%%log_path:\=%separator%%%

	REM Do the same for analytics data file path
	set usage_data_path=%GWHOME%\api-usage-data
	call set usage_data_path=%%usage_data_path:\=%separator%%%

	if %verbose%==T echo [%date% %time%] DEBUG: b7a.http.accesslog.path = "%GWHOME%\logs\access_logs"
	if %verbose%==T echo [%date% %time%] DEBUG: configs = %unix_style_path%
	if %verbose%==T echo [%date% %time%] DEBUG: Starting micro gateway server...

	REM Check if powershell is available
	WHERE powershell >nul 2>nul
	IF %ERRORLEVEL% NEQ 0 (
		echo [%date% %time%] WARN: Can't find powershell in the system!
		echo [%date% %time%] WARN: STDERR and STDOUT will be piped to %GWHOME%\logs\microgateway.log
		REM To append to existing logs used `>>` to redirect STDERR to STDOUT used `2>&1`
		%GWHOME%\runtime\bin\ballerina run -e api.usage.data.path=%usage_data_path%  -e b7a.http.accesslog.path=%unix_style_path% --config "%GWHOME%\conf\micro-gw.conf" "%*" >> "%GWHOME%\logs\microgateway.log" 2>&1
	) else (
		REM Change Java heap Xmx and Xmx values
		powershell -Command "(Get-Content %GWHOME%\runtime\bin\ballerina.bat) | Foreach-Object {$_ -replace 'Xms.*?m','Xms%JAVA_XMS_VALUE% '} | Foreach-Object {$_ -replace 'Xmx.*?m','Xmx%JAVA_XMX_VALUE% '} | Set-Content %GWHOME%\runtime\bin\ballerina_1.bat"
		powershell -Command "Remove-Item %GWHOME%\runtime\bin\ballerina.bat"
		powershell -Command "Rename-Item -path %GWHOME%\runtime\bin\ballerina_1.bat -newName ballerina.bat"
		CD %GWHOME%
		for /f "skip=3 tokens=2 delims=:" %%A in ('powershell -command "get-host"') do (
			set /a n=!n!+1
			set c=%%A
			if !n!==1 set PSVersion=!c!
		)
		set PSVersion=!PSVersion: =!
		if %verbose%==T echo [%date% %time%] DEBUG: PowerShell version !PSVersion! detected!
		set PSVersion=!PSVersion:~0,1!
		echo [%date% %time%] Starting Micro-Gateway
		IF !PSVersion! LEQ 3 (
			echo [%date% %time%] Starting Micro-Gateway >>  .\logs\microgateway.log
			call powershell ".\runtime\bin\ballerina run -e api.usage.data.path=%usage_data_path% -e b7a.http.accesslog.path=%unix_style_path% --config .\conf\micro-gw.conf | out-file -encoding ASCII -filepath .\logs\microgateway.log -Append"
		 ) else (
			REM For powershell version 4 or above , We can use `tee` command for output to both file stream and stdout (Ref: https://en.wikipedia.org/wiki/PowerShell#PowerShell_4.0)
			call powershell ".\runtime\bin\ballerina run -e api.usage.data.path=%usage_data_path% -e b7a.http.accesslog.path=%unix_style_path% --config .\conf\micro-gw.conf | tee -Append .\logs\microgateway.log"
		)
	)
:end
goto endlocal

:endlocal

:END
