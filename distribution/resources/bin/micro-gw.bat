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
REM Startup Script for Gateway Cli
REM
REM Environment Variable Prerequisites
REM
REM   BALLERINA_HOME      Home of Ballerina installation.
REM
REM   JAVA_HOME           Must point at your Java Development Kit installation.
REM
REM   JAVA_OPTS           (Optional) Java runtime options used when the commands
REM                       is executed.
REM
REM NOTE: Borrowed generously from Apache Tomcat startup scripts.
REM -----------------------------------------------------------------------------
setlocal EnableDelayedExpansion

if ""%1%""==""-v"" ( set verbose=T ) else ( set verbose=F )
if %verbose%==T ( echo Verbose mode enabled )

REM Get the location of this(micro-gw.bat) file
SET PRGDIR=%~dp0
set CURRENT_D=%CD%
if "%MICROGW_TOOLKIT_HOME%" == "" set MICROGW_TOOLKIT_HOME=%PRGDIR%..

REM  set BALLERINA_HOME
set BALLERINA_HOME=%MICROGW_TOOLKIT_HOME%\lib\platform
set PATH=%PATH%;%BALLERINA_HOME%\bin\
if %verbose%==T echo BALLERINA_HOME environment variable is set to %BALLERINA_HOME%

echo MICROGW_TOOLKIT_HOME environment variable is set to %MICROGW_TOOLKIT_HOME%

rem Check JAVA availability
:checkJava
	if "%JAVA_HOME%" == "" goto noJavaHome
	if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
	goto checkJava

:noJavaHome
	echo "You must set the JAVA_HOME variable before running Micro-Gateway Tooling."
goto end

:checkJava
	"%JAVA_HOME%\bin\java" -version
	IF ERRORLEVEL 1 goto noJava
	goto runServer

:noJava
	echo Error: JAVA_HOME is not defined correctly.
goto end

:runServer
	echo JAVA_HOME environment variable was set to %JAVA_HOME%
	set originalArgs=%*
	if ""%1""=="""" goto usageInfo

rem Slurp the command line arguments. This loop allows for an unlimited number
rem of arguments (up to the command line limit, anyway).
:setupArgs
	if %verbose%==T echo [%date% %time%] DEBUG: Processing argument : `%1`
	if ""%1""=="""" goto passToJar

	if ""%1""==""build""     goto commandBuild

	if ""%1""==""-java.debug""    goto commandDebug
	if ""%1""==""java.debug""   goto commandDebug
	if ""%1""==""--java.debug""  goto commandDebug

	shift
goto setupArgs

:usageInfo
	echo Missing command operand
	echo "Usage: micro-gw [-v] ([ -l | --label ] setup | build | run)"
goto :end

:commandBuild
	if %verbose%==T echo [%date% %time%] DEBUG: Running commandSetup

	set found=false
	for %%a in (!originalArgs!) do (
		if %verbose%==T echo [%date% %time%] DEBUG: looking for -l or --label in `%%a` found = %found%
		if !found!==true (
			set label=%%a
			goto :labelFound
		)
		:: Below if conditions will break if simplify into one line
		if %%a==-label (
			set found=true
			)
		if %%a==-l (
			set found=true
			)
	)
	if !found!==false goto :usageInfo

	:labelFound
		if %verbose%==T echo [%date% %time%] DEBUG: Building micro gateway for label %label%

		REM Set micro gateway project directory relative to CD (current directory)
		set MICRO_GW_LABEL_PROJECT_DIR="%CURRENT_D%\micro-gw-resources\projects\%label%"
		if exist %MICRO_GW_LABEL_PROJECT_DIR% goto :continueBuild
			REM Exit, if can not find a project with given label
			if %verbose%==T echo [%date% %time%] DEBUG: Project directory does not exist for given label %MICRO_GW_LABEL_PROJECT_DIR%
			echo "Incorrect label `%label%` or Workspace not initialized, Run setup befor building the project!"
			goto :EOF

	:continueBuild
		pushd "%MICRO_GW_LABEL_PROJECT_DIR%"
			if %verbose%==T echo [%date% %time%] DEBUG: current dir %CD%
			set TARGET_DIR="%MICRO_GW_LABEL_PROJECT_DIR%\target"
			:: /s : Removes the specified directory and all subdirectories including any files. Use /s to remove a tree.
			:: /q : Runs rmdir in quiet mode. Deletes directories without confirmation.
			if exist "%TARGET_DIR%"  ( rmdir "%TARGET_DIR%" /s /q )
			ballerina build src -o %label%.balx
		popd
		if %verbose%==T echo [%date% %time%] DEBUG: Ballerina build completed
goto :end

:commandDebug
	if %verbose%==T echo [%date% %time%] DEBUG: Running commandDebug

	shift
	set DEBUG_PORT=%1
	if "%DEBUG_PORT%"=="" goto noDebugPort
	if not "%JAVA_OPTS%"=="" echo Warning !!!. User specified JAVA_OPTS will be ignored, once you give the --debug option.
	set JAVA_OPTS=-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=%DEBUG_PORT%
	echo Please start the remote debugging client to continue...
goto passToJar

:noDebugPort
	echo Please specify the debug port after the java.debug option
goto end


:passToJar
	rem ---------- Add jars to classpath ----------------
	if %verbose%==T echo [%date% %time%] DEBUG: Running passToJar

	set CLI_CLASSPATH=
	if exist "%BALLERINA_HOME%"\bre\lib ( 
		for %%i in ("%BALLERINA_HOME%"\bre\lib\*.jar) do (
			set CLI_CLASSPATH=!CLI_CLASSPATH!;.\lib\platform\bre\lib\%%~ni%%~xi
		)
	)

	if %verbose%==T echo [%date% %time%] DEBUG: CLI_CLASSPATH = "%CLI_CLASSPATH%"

	set JAVACMD=-Xms256m -Xmx1024m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath="%MICROGW_TOOLKIT_HOME%\heap-dump.hprof" %JAVA_OPTS% -classpath %CLI_CLASSPATH% -Djava.security.egd=file:/dev/./urandom -Dballerina.home="%BALLERINA_HOME%" -Djava.util.logging.config.class="org.ballerinalang.logging.util.LogConfigReader" -Djava.util.logging.manager="org.ballerinalang.logging.BLogManager" -Dfile.encoding=UTF8 -Dcli.home="%MICROGW_TOOLKIT_HOME%" -Dtemplates.dir.path=.\resources\templates
	if %verbose%==T echo [%date% %time%] DEBUG: JAVACMD = !JAVACMD!

:runJava
	REM Jump to GW-CLI exec location when running the jar
	CD %MICROGW_TOOLKIT_HOME%
	"%JAVA_HOME%\bin\java" %JAVACMD% org.wso2.apimgt.gateway.cli.cmd.Main %originalArgs% --path %CURRENT_D%
	if "%ERRORLEVEL%"=="121" goto runJava
:end
goto endlocal

:endlocal

:END
