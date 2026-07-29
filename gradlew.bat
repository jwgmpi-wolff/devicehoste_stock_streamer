@rem
@rem Copyright 2015 the original author or authors.
@rem Licensed under the Apache License, Version 2.0
@rem

@if "%DEBUG%"=="" @echo off
@rem Set local scope
setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem Remove trailing slash
set APP_HOME=%DIRNAME:~0,-1%

@rem Resolve JAVA_HOME
if defined JAVA_HOME goto findJavaFromJavaHome
set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot
:findJavaFromJavaHome
set JAVA_EXE=%JAVA_HOME%/bin/java.exe
if exist "%JAVA_EXE%" goto execute
echo JAVA_HOME not set or not found. Tried: %JAVA_HOME%
exit /b 1

:execute
"%JAVA_EXE%" -classpath "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*

:end
endlocal
