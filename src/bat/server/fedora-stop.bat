@echo off

goto checkEnv

:envOk

set TOMCAT_DIR=@tomcat.basename@
set TC=%FEDORA_HOME%\server\%TOMCAT_DIR%
set OLD_JAVA_HOME=%JAVA_HOME%
set JAVA_HOME=%THIS_JAVA_HOME%

:undeploy

set C=%TC%\common\lib
set CP=%C%\saaj.jar;%C%\commons-discovery.jar;%C%\axis.jar;%C%\commons-logging.jar;%C%\jaxrpc.jar;%C%\wsdl4j.jar;%C%\tt-bytecode.jar

echo Shutting down Fedora Servers and associated Modules...
"%JAVA_HOME%\bin\java" -cp %TC%\webapps\fedora\WEB-INF\classes;%TC%\common\lib\servlet.jar -Dfedora.home=%FEDORA_HOME% fedora.server.ServerController shutdown

echo Shutting down Fedora-Server service...
"%JAVA_HOME%\bin\java" -cp %TC%\bin\bootstrap.jar -Dfedora.home=%FEDORA_HOME% -Dclasspath=%TC%\bin\bootstrap.jar -Djava.endorsed.dirs=%TC%\bin -Djava.security.manager -Djava.security.policy=%TC%\conf\catalina.policy -Dcatalina.base=%TC% -Dcatalina.home=%TC% -Djava.io.tmpdir=%TC%\temp org.apache.catalina.startup.Bootstrap stop
set JAVA_HOME=%OLD_JAVA_HOME%

goto end

:checkEnv
if "%FEDORA_HOME%" == "" goto noFedoraHome
if not exist %FEDORA_HOME%\server\config\fedora.fcfg goto configNotFound
if "%FEDORA_JAVA_HOME%" == "" goto tryJavaHome
set THIS_JAVA_HOME=%FEDORA_JAVA_HOME%
:checkJava
if not exist %THIS_JAVA_HOME%\bin\java.exe goto noJavaBin
if not exist %THIS_JAVA_HOME%\bin\orbd.exe goto badJavaVersion
goto envOk

:tryJavaHome
if "%JAVA_HOME%" == "" goto noJavaHome
set THIS_JAVA_HOME=%JAVA_HOME%
goto checkJava

:noFedoraHome
echo ERROR: Environment variable, FEDORA_HOME must be set.
goto end

:configNotFound
echo ERROR: FEDORA_HOME does not appear correctly set.
echo Configuration cannot be found at %FEDORA_HOME%\server\config\fedora.fcfg
goto end

:noJavaHome
echo ERROR: FEDORA_JAVA_HOME was not defined, nor was (the fallback) JAVA_HOME.
goto end

:noJavaBin
echo ERROR: java.exe was not found in %THIS_JAVA_HOME%
echo Make sure FEDORA_JAVA_HOME or JAVA_HOME is set correctly.
goto end

:badJavaVersion
echo ERROR: java was found in %THIS_JAVA_HOME%, but it was not version 1.4
echo Make sure FEDORA_JAVA_HOME or JAVA_HOME points to a 1.4JRE/JDK base.
goto end

:end

