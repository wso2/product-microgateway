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
SET PRGDIR=%~sdp0
set CURRENT_D=%CD%

REM If the current disk drive ie: `E:\` is different from the drive where this (micro-gw.bat) resides(i:e `C:\`), Change the driver label the current drive
:switchDrive
	SET curDrive=%CURRENT_D:~0,1%
	SET wsasDrive=%PRGDIR:~0,1%
	if %verbose%==T ( echo Switch to drive '%wsasDrive%' if current drive '%curDrive%' not equal to program drive '%wsasDrive%' )
	if not "%curDrive%" == "%wsasDrive%" %wsasDrive%:

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
	if ""%1""==""help""     goto passToJar

	if ""%1""==""build""     goto commandBuild

	if ""%1""==""-java.debug""    goto commandDebug
	if ""%1""==""java.debug""   goto commandDebug
	if ""%1""==""--java.debug""  goto commandDebug
	shift
goto setupArgs

:usageInfo
	echo Missing command operand
	echo "Usage: micro-gw [-v] ([ -l ] setup | build)"
goto :end

:commandBuild
	if %verbose%==T echo [%date% %time%] DEBUG: Running commandBuild

	REM Immediate next parameter should be project name after the `build` command
	shift
	set "project_name=%1"
	if [%project_name%] == [] ( goto :noName ) else ( goto :nameFound )

	:noName
		echo "Project name not provided please follow the command usage patterns given below"
		goto :usageInfo

	:nameFound
		if %verbose%==T echo [%date% %time%] DEBUG: Building micro gateway for project %project_name%

		REM Set micro gateway project directory relative to CD (current directory)
		set MICRO_GW_PROJECT_DIR="%CURRENT_D%\%project_name%"
		if exist %MICRO_GW_PROJECT_DIR% goto :continueBuild
			REM Exit, if can not find a project with given project name
			if %verbose%==T echo [%date% %time%] DEBUG: Project directory does not exist for given name %MICRO_GW_PROJECT_DIR%
			echo "Incorrect project name `%project_name%` or Workspace not initialized, Run setup befor building the project!"
			goto :EOF

	:continueBuild
		pushd "%MICRO_GW_PROJECT_DIR%"
			if %verbose%==T echo [%date% %time%] DEBUG: current dir %CD%
			set TARGET_DIR="%MICRO_GW_PROJECT_DIR%\target"
			:: /s : Removes the specified directory and all subdirectories including any files. Use /s to remove a tree.
			:: /q : Runs rmdir in quiet mode. Deletes directories without confirmation.
			if exist "%TARGET_DIR%"  ( rmdir "%TARGET_DIR%" /s /q )
			call ballerina build src -o %project_name%.balx
		popd
		if %verbose%==T echo [%date% %time%] DEBUG: Ballerina build completed
		REM Check for a debug param by looping through the remaining args list
		:checkDebug
			shift
			if ""%1""=="""" goto passToJar

			if ""%1""==""-java.debug""    goto commandDebug
			if ""%1""==""java.debug""   goto commandDebug
			if ""%1""==""--java.debug""  goto commandDebug
		goto checkDebug
goto :passToJar

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

	set JAVACMD=-Xms256m -Xmx1024m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath="%MICROGW_TOOLKIT_HOME%\heap-dump.hprof" %JAVA_OPTS% -classpath %CLI_CLASSPATH% -Djava.security.egd=file:/dev/./urandom -Dballerina.home="%BALLERINA_HOME%" -Djava.util.logging.config.class="org.ballerinalang.logging.util.LogConfigReader" -Djava.util.logging.manager="org.ballerinalang.logging.BLogManager" -Dfile.encoding=UTF8 -Dcli.home="%MICROGW_TOOLKIT_HOME%" -Dtemplates.dir.path=.\resources\templates -Dcurrent.dir=%CURRENT_D%
	if %verbose%==T echo [%date% %time%] DEBUG: JAVACMD = !JAVACMD!

:runJava
	REM Jump to GW-CLI exec location when running the jar
	CD %MICROGW_TOOLKIT_HOME%
	"%JAVA_HOME%\bin\java" %JAVACMD% org.wso2.apimgt.gateway.cli.cmd.Main %originalArgs%
	if "%ERRORLEVEL%"=="121" goto runJava
:end
goto endlocal

:endlocal

:END
