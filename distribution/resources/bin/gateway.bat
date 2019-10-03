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

REM Set Java Xms and Xmx values. The values specified here will set in gateway runtime.
SET JAVA_XMS_VALUE="256m"
SET JAVA_XMX_VALUE="512m"

REM Get the location of this(gateway.bat) file
SET PRGDIR=%~dp0
SET GWHOME=%PRGDIR%..
REM  set BALLERINA_HOME
set BALLERINA_HOME=%GWHOME%\runtime
set JAVA_HOME=%GWHOME%\lib\jdk8u202-b08-jre

REM Check if path to runtime executable is available
set last=""
for %%a in (%*) do set last=%%a
if %last%=="" set isInvalidPath=T
if not exist %last% set isInvalidPath=T
if "%isInvalidPath%"=="T" (
	echo Path to executable balx file is invalid
    goto end
)

REM Extract ballerina runtime
if not exist %GW_HOME%\runtime\ (
    REM TODO: Evaluate the use of powershell `tee` here
    call "%PRGDIR%\tools.exe"
    if ERRORLEVEL 0 (
        xcopy /y "%GWHOME%\lib\gateway\*.jar" "%GWHOME%\runtime\bre\lib\" >nul
        xcopy /sy "%GWHOME%\lib\gateway\balo\wso2" "%GWHOME%\runtime\lib\repo\wso2\" >nul
    )
)

REM Needs to identify the ballerina arguments and the last argument which is the path of executable.
REM The path of executable should be provided as \"<path>\" to avoid ballerina when the path includes a space.
REM BAL_ARGS variable is used to store formatted string
set BAL_ARGS=
:formatAndValidateCmdArgs
    if "%~1"=="-e" (
        set "BAL_ARGS=%BAL_ARGS% %1 %2=%3"
        shift
        shift
        shift
        goto :formatAndValidateCmdArgs
    ) else (
        if "%~2"=="" (
            set "BAL_ARGS=%BAL_ARGS% \"%~1\""
            goto :callBallerina
        ) else (
            if "%~1"=="--debug" (
                set "BAL_ARGS=%BAL_ARGS% %1 %2"
                    shift
                    shift
                    goto :formatAndValidateCmdArgs
            ) else (
                    echo %*
                    echo "Provided set of arguments are invalid."
                    goto end
            )
        )
    )

REM Slurp the command line arguments. This loop allows for an unlimited number
REM of arguments (up to the command line limit, anyway).
:setupArgs
	if ""%1""=="""" goto :formatAndValidateCmdArgs
	if ""%1""==""--debug""    goto commandDebug
	shift
goto setupArgs

:commandDebug
	shift
	set DEBUG_PORT=%1
	if "%DEBUG_PORT%"=="" goto noDebugPort
	echo Please start the remote debugging client to continue...
goto :formatAndValidateCmdArgs

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

	REM Check if powershell is available
	WHERE powershell >nul 2>nul
	IF %ERRORLEVEL% NEQ 0 (
		echo [%date% %time%] WARN: Can't find powershell in the system!
		echo [%date% %time%] WARN: All messages will be redirected to %GWHOME%\logs\microgateway.log
		REM To append to existing logs used `>>` to redirect STDERR to STDOUT used `2>&1`
		"%GWHOME%\runtime\bin\ballerina" run -e api.usage.data.path=%usage_data_path%  -e b7a.http.accesslog.path=%unix_style_path% --config "%GWHOME%\conf\micro-gw.conf" %BAL_ARGS% >> "%GWHOME%\logs\microgateway.log" 2>&1
	) else (
		REM Change Java heap Xmx and Xmx values
		powershell -Command "(Get-Content \"%GWHOME%\runtime\bin\ballerina.bat\") | Foreach-Object {$_ -replace 'Xms.*?m','Xms%JAVA_XMS_VALUE% '} | Foreach-Object {$_ -replace 'Xmx.*?m','Xmx%JAVA_XMX_VALUE% '} | Set-Content \"%GWHOME%\runtime\bin\ballerina_1.bat\""
		powershell -Command "Remove-Item \"%GWHOME%\runtime\bin\ballerina.bat\""
		powershell -Command "Rename-Item -path \"%GWHOME%\runtime\bin\ballerina_1.bat\" -newName ballerina.bat"
		CD "%GWHOME%"
		SET PSVersion=1
		for /f "tokens=*" %%A in ('powershell -command "$PSVersionTable.PSVersion.Major"') do (set PSVersion=%%A)
		REM Remove whitespaces from PSVersion
		set PSVersion=!PSVersion: =!

		IF !PSVersion! LEQ 3 (
			call powershell ".\runtime\bin\ballerina run -e api.usage.data.path=\"%usage_data_path%\" -e b7a.http.accesslog.path=\"%unix_style_path%\" --config .\conf\micro-gw.conf %BAL_ARGS% | out-file -filepath .\logs\microgateway.log -Append"
		 ) else (
			REM For powershell version 4 or above , We can use `tee` command for output to both file stream and stdout (Ref: https://en.wikipedia.org/wiki/PowerShell#PowerShell_4.0)
			call powershell ".\runtime\bin\ballerina run -e api.usage.data.path=\"%usage_data_path%\" -e b7a.http.accesslog.path=\"%unix_style_path%\" --config .\conf\micro-gw.conf %BAL_ARGS% | tee -Append .\logs\microgateway.log"
		)
	)
:end
goto endlocal

:endlocal

:END
