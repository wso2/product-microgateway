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
REM NOTE: Borrowed generously from Apache Tomcat startup scripts.
REM -----------------------------------------------------------------------------
setlocal EnableDelayedExpansion

REM -----------------------------------------------------------------------------
REM --- START OF GLOBAL VARIABLES ---
REM -----------------------------------------------------------------------------

REM Set Java Xms and Xmx values. The values specified here will set in gateway runtime.
SET JAVA_XMS_VALUE=256m
SET JAVA_XMX_VALUE=512m

REM Set global variables
SET PRGDIR=%~dp0
SET GW_HOME=%PRGDIR%..
SET MGW_VERSION="3.1.0"
SET CONF_FILE="%GW_HOME%\conf\micro-gw.conf"
SET CONF_OUT_FILE="%GW_HOME%\.config"
SET IS_METRICS_ENABLED=F
SET EXEC_FILE=
SET BAL_ARGS=
REM If java_home is set and version is 1.8 in the running environment,
REM pick that as the java_home for MGW. If not set internal jre home
IF EXIST "%JAVA_HOME%" (
    SET JAVA_CMD="%JAVA_HOME%\bin\java.exe"
    SET JAVA_VERSION=
    FOR /F "tokens=* USEBACKQ" %%F IN (`%JAVA_CMD% -fullversion 2^>^&1`) DO (
        SET JAVA_VERSION=%%F
    )

    REM External java_home was detected, now check if it is java8
    ECHO "%JAVA_VERSION%"|find "1.8." >NUL
    IF %ERRORLEVEL% NEQ 0 SET JAVA_HOME=%GW_HOME%\lib\jdk8u202-b08-jre
) ELSE SET JAVA_HOME=%GW_HOME%\lib\jdk8u202-b08-jre

REM -----------------------------------------------------------------------------
REM --- END OF GLOBAL VARIABLES ---
REM -----------------------------------------------------------------------------

REM -----------------------------------------------------------------------------
REM --- START OF MAIN PROGRAM LOGIC ---
REM -----------------------------------------------------------------------------

REM Check for verssion command
IF "%~1"=="version" (
    IF EXIST %GW_HOME%\version.txt (
        type "%GW_HOME%\version.txt"
        EXIT/B 0
    )
)

CALL :checkJava
IF %ERRORLEVEL% NEQ 0 GOTO END

CALL :validateExecutable %*
IF %ERRORLEVEL% NEQ 0 GOTO END

SET EXEC_FILE=%~1
CALL :buildBalArgs %*

CALL :runTools %CONF_FILE% %CONF_OUT_FILE%

IF "%b7a_observability_metrics_enabled%"=="true" (
    SET IS_METRICS_ENABLED=T
) ELSE (
    CAll :isMetricsEnabled %*
)

IF EXIST %CONF_OUT_FILE% (
    IF "%IS_METRICS_ENABLED%"=="F" (
        FOR /F "delims=" %%i IN (%CONF_OUT_FILE%) DO (
            IF %%i==true SET IS_METRICS_ENABLED=T
            REM Prevent falling into else block
            GOTO :enableAgent
        )
    )
)
:enableAgent
    IF "%IS_METRICS_ENABLED%"=="T" (
        FOR /F "skip=1 delims=" %%i IN (%CONF_OUT_FILE%) DO (
            SET jmxPort=%%i
            GOTO :setJavaOpts
        )
        :setJavaOpts
            SET JAVA_OPTS="-javaagent:%GW_HOME%\lib\gateway\jmx_prometheus_javaagent-0.12.0.jar=%jmxPort%:%GW_HOME%\conf\Prometheus\config.yml"
    )

IF EXIST %CONF_OUT_FILE% DEL /Q /F %CONF_OUT_FILE%

:continueInit
    REM Change the windows style `\` path separator to unix style `/path/to/file` for log file path
    SET "separator=/"
    SET log_path="%GW_HOME%\logs\access_logs"
    CALL SET ACCESS_LOG_PATH=%%log_path:\=%separator%%%

    REM Do the same for analytics data file path
    SET usage_path=%GW_HOME%\api-usage-data
    CALL SET USAGE_DATA_PATH=%%usage_path:\=%separator%%%

    CALL :startGateway %*

    GOTO END

REM -----------------------------------------------------------------------------
REM --- END OF MAIN PROGRAM LOGIC ---
REM -----------------------------------------------------------------------------

REM -----------------------------------------------------------------------------
REM --- START OF FUNCTION DEFINITION ---
REM -----------------------------------------------------------------------------

REM Start the gateway using internal ballerina distribution as the runtime
:startGateway
    REM Check if powershell is available
    WHERE POWERSHELL >NUL 2>NUl

    IF %ERRORLEVEL% NEQ 0 (
        ECHO WARN: Can't find powershell in the system!
        ECHO WARN: STDERR and STDOUT will be piped to %GW_HOME%\logs\microgateway.log
        SET JAVA_ARGS=-Xms%JAVA_XMS_VALUE% -Xmx%JAVA_XMX_VALUE% -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath="%GW_HOME%\heap-dump.hprof"
        "%JAVA_HOME%\bin\java.exe" %JAVA_ARGS% -Dmgw-runtime.home"=%GW_HOME%" -Dballerina.home="%GW_HOME%/runtime" -Djava.util.logging.config.class=org.ballerinalang.logging.util.LogConfigReader -Djava.util.logging.manager=org.ballerinalang.logging.BLogManager -jar "%EXEC_FILE%" %BAL_ARGS% --api.usage.data.path="%USAGE_DATA_PATH%" --b7a.config.file="%GW_HOME%\conf\micro-gw.conf" >> "%GW_HOME%\logs\microgateway.log" 2>&1

        EXIT /B %ERRORLEVEL%
    ) ELSE (
        SET JAVA_ARGS=-Xms%JAVA_XMS_VALUE% -Xmx%JAVA_XMX_VALUE% -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath='%GW_HOME%\heap-dump.hprof'
        REM Get short path for java_home in case java_home was picked from a
        REM standard installation dir with space in the path ex: "program files"
        FOR %%I IN ("%JAVA_HOME%") DO SET JAVA_HOME=%%~sI
        FOR /f "skip=3 tokens=2 delims=:" %%A IN ('powershell -command "get-host"') DO (
            SET /a n=!n!+1
            SET c=%%A
            IF !n!==1 SET PSVersion=!c!
        )
        SET PSVersion=!PSVersion: =!
        SET PSVersion=!PSVersion:~0,1!

        REM TODO: Possible solution for this complexity can be Add-Content Cmdlet. Do some RnD on it.
        IF !PSVersion! LEQ 3 (
            CALL POWERSHELL "!JAVA_HOME!\bin\java.exe %JAVA_ARGS% '-Dmgw-runtime.home=%GW_HOME%' '-Dballerina.home=%GW_HOME%/runtime' '-Djava.util.logging.config.class=org.ballerinalang.logging.util.LogConfigReader' '-Djava.util.logging.manager=org.ballerinalang.logging.BLogManager' -jar '%EXEC_FILE%' %BAL_ARGS% --api.usage.data.path='%USAGE_DATA_PATH%' --b7a.config.file='%GW_HOME%\conf\micro-gw.conf' | out-file -encoding Unicode -filepath '%GW_HOME%\logs\microgateway.log' -Append"
            EXIT /B %ERRORLEVEL%
        ) ELSE (
            REM For powershell version 4 or above , We can use `tee` command for output to both file stream and stdout (Ref: https://en.wikipedia.org/wiki/PowerShell#PowerShell_4.0)
            CALL POWERSHELL "!JAVA_HOME!\bin\java.exe %JAVA_ARGS% '-Dmgw-runtime.home=%GW_HOME%' '-Dballerina.home=%GW_HOME%/runtime' '-Djava.util.logging.config.class=org.ballerinalang.logging.util.LogConfigReade' '-Djava.util.logging.manager=org.ballerinalang.logging.BLogManager' -jar '%EXEC_FILE%' %BAL_ARGS% --api.usage.data.path='%USAGE_DATA_PATH%' --b7a.config.file='%GW_HOME%\conf\micro-gw.conf' | tee -Append '%GW_HOME%\logs\microgateway.log'
            EXIT /B %ERRORLEVEL%
        )
    )

REM Validate the provided runtime artifact
:validateExecutable
    REM Check if path to runtime executable is valid
    SET isInvalidPath=F
    SET xPath="%1"
    IF "%xPath%"=="" SET isInvalidPath=T
    REM Path should exists
    IF EXIST "%xPath%" (
        REM Path should not be a directory
        IF EXIST %xPath%/* SET isInvalidPath=T
    ) ELSE SET isInvalidPath=T

    IF "%isInvalidPath%"=="T" (
        ECHO Path to executable jar file is invalid
        EXIT /B 1
    )

    EXIT /B 0

REM Run a command on external tool library
REM arg0: command and other arguments to pass to tool library
:runTools
    SET METRIC_CLASSPATH="%GW_HOME%\lib\gateway\*"
    SET JAVA_CMD=-Xms256m -Xmx1024m ^
        -XX:+HeapDumpOnOutOfMemoryError ^
        -XX:HeapDumpPath="%GW_HOME%\heap-dump.hprof" ^
        %JAVA_OPTS% ^
        -classpath %METRIC_CLASSPATH%

    "%JAVA_HOME%\bin\java.exe" %JAVA_CMD% org.wso2.micro.gateway.tools.Main "%~1" "%~2"

    EXIT /B %ERRORLEVEL%

REM Check JAVA availability
:checkJava
    IF "%JAVA_HOME%"=="" (
        ECHO ERROR: JAVA_HOME is invalid.
        EXIT /B 1
    )
    IF NOT EXIST "%JAVA_HOME%\bin\java.exe" (
        ECHO ERROR: JAVA_HOME is invalid.
        EXIT /B 1
    )
    EXIT /B 0

REM Build the list of arguments for ballerina
REM This has to be done in order to avoid issues when exec jar file path contain spaces
REM We need to issolate the jar file path and wrap it with quotes
:buildBalArgs
    SHIFT
    SET first=%~1
    IF "%first%"=="" EXIT /B 0
    IF "%first:~0,2%"=="--" (
        SET BAL_ARGS=%BAL_ARGS% %first%=%2
        SHIFT
    ) ELSE (
        SET BAL_ARGS=%BAL_ARGS% %first%
    )
    GOTO :buildBalArgs

REM Find metrics is enabled or not via cmd args
:isMetricsEnabled
    SET first=%~1
    IF "%first%"=="" EXIT /B 0
    SET keyFound=F
    IF "%first%"=="--b7a.observability.enabled" (
        SET keyFound=T
    ) ELSE "%first%"=="--b7a.observability.metrics.enabled" (
        SET keyFound=T
    )

    IF "%keyFound%"=="T" (
        IF "%~2"=="true" SET IS_METRICS_ENABLED=T
        EXIT /B 0
    )
    IF "%first:~0,2%"=="--" (
        SHIFT
        SHIFT
    ) ELSE (
        SHIFT
    )
    GOTO :isMetricsEnabled

REM -----------------------------------------------------------------------------
REM --- END OF FUNCTION DEFINITION ---
REM -----------------------------------------------------------------------------

:END
    EXIT /B %ERRORLEVEL%
