@echo off

goto checkEnv
:envOk

set OLD_JAVA_HOME=%JAVA_HOME%
set JAVA_HOME=%THIS_JAVA_HOME%

:runMinimized

echo Ingesting Demonstration Objects (26 total)...

echo Ingesting local-server simple image demo (1 bdef, 1 bmech, 1 object)...
%JAVA_HOME%\bin\java -Xms64m -Xmx96m -cp %FEDORA_HOME%\client;%FEDORA_HOME%\client\client.jar -Dfedora.home=%FEDORA_HOME% fedora.client.ingest.AutoIngestor %1 %2 %3 %4 %FEDORA_HOME%\demo\local-server-demos\simple-image-demo\bdef-simple-image.xml "Created by fedora-ingest-demos script"
%JAVA_HOME%\bin\java -Xms64m -Xmx96m -cp %FEDORA_HOME%\client;%FEDORA_HOME%\client\client.jar -Dfedora.home=%FEDORA_HOME% fedora.client.ingest.AutoIngestor %1 %2 %3 %4 %FEDORA_HOME%\demo\local-server-demos\simple-image-demo\bmech-simple-image-4res.xml "Created by fedora-ingest-demos script"
%JAVA_HOME%\bin\java -Xms64m -Xmx96m -cp %FEDORA_HOME%\client;%FEDORA_HOME%\client\client.jar -Dfedora.home=%FEDORA_HOME% fedora.client.ingest.AutoIngestor %1 %2 %3 %4 %FEDORA_HOME%\demo\local-server-demos\simple-image-demo\obj-image-4res-colliseum.xml "Created by fedora-ingest-demos script"

echo Ingesting local-server simple document demo (1 object)...
%JAVA_HOME%\bin\java -Xms64m -Xmx96m -cp %FEDORA_HOME%\client;%FEDORA_HOME%\client\client.jar -Dfedora.home=%FEDORA_HOME% fedora.client.ingest.AutoIngestor %1 %2 %3 %4 %FEDORA_HOME%\demo\local-server-demos\simple-document-demo\obj-document-ECDLpaper.xml "Created by fedora-ingest-demos script"

echo Ingesting local-server document transform demo (1 bdef, 1 bmech, 1 object)...
%JAVA_HOME%\bin\java -Xms64m -Xmx96m -cp %FEDORA_HOME%\client;%FEDORA_HOME%\client\client.jar -Dfedora.home=%FEDORA_HOME% fedora.client.ingest.AutoIngestor %1 %2 %3 %4 %FEDORA_HOME%\demo\local-server-demos\document-transform-demo\bdef-document-trans.xml "Created by fedora-ingest-demos script"
%JAVA_HOME%\bin\java -Xms64m -Xmx96m -cp %FEDORA_HOME%\client;%FEDORA_HOME%\client\client.jar -Dfedora.home=%FEDORA_HOME% fedora.client.ingest.AutoIngestor %1 %2 %3 %4 %FEDORA_HOME%\demo\local-server-demos\document-transform-demo\bmech-document-trans-saxon.xml "Created by fedora-ingest-demos script"
%JAVA_HOME%\bin\java -Xms64m -Xmx96m -cp %FEDORA_HOME%\client;%FEDORA_HOME%\client\client.jar -Dfedora.home=%FEDORA_HOME% fedora.client.ingest.AutoIngestor %1 %2 %3 %4 %FEDORA_HOME%\demo\local-server-demos\document-transform-demo\obj-document-fedoraAPIA.xml "Created by fedora-ingest-demos script"

echo Ingesting local-server formatting objects demo (2 bdefs, 3 bmechs, 3 objects)...
%JAVA_HOME%\bin\java -Xms64m -Xmx96m -cp %FEDORA_HOME%\client;%FEDORA_HOME%\client\client.jar -Dfedora.home=%FEDORA_HOME% fedora.client.ingest.AutoIngestor %1 %2 %3 %4 %FEDORA_HOME%\demo\local-server-demos\formatting-objects-demo\bdef-fo.xml "Created by fedora-ingest-demos script"
%JAVA_HOME%\bin\java -Xms64m -Xmx96m -cp %FEDORA_HOME%\client;%FEDORA_HOME%\client\client.jar -Dfedora.home=%FEDORA_HOME% fedora.client.ingest.AutoIngestor %1 %2 %3 %4 %FEDORA_HOME%\demo\local-server-demos\formatting-objects-demo\bdef-pdf.xml "Created by fedora-ingest-demos script"
%JAVA_HOME%\bin\java -Xms64m -Xmx96m -cp %FEDORA_HOME%\client;%FEDORA_HOME%\client\client.jar -Dfedora.home=%FEDORA_HOME% fedora.client.ingest.AutoIngestor %1 %2 %3 %4 %FEDORA_HOME%\demo\local-server-demos\formatting-objects-demo\bmech-dbx-to-fo.xml "Created by fedora-ingest-demos script"
%JAVA_HOME%\bin\java -Xms64m -Xmx96m -cp %FEDORA_HOME%\client;%FEDORA_HOME%\client\client.jar -Dfedora.home=%FEDORA_HOME% fedora.client.ingest.AutoIngestor %1 %2 %3 %4 %FEDORA_HOME%\demo\local-server-demos\formatting-objects-demo\bmech-fop.xml "Created by fedora-ingest-demos script"
%JAVA_HOME%\bin\java -Xms64m -Xmx96m -cp %FEDORA_HOME%\client;%FEDORA_HOME%\client\client.jar -Dfedora.home=%FEDORA_HOME% fedora.client.ingest.AutoIngestor %1 %2 %3 %4 %FEDORA_HOME%\demo\local-server-demos\formatting-objects-demo\bmech-tei-to-fo.xml "Created by fedora-ingest-demos script"
%JAVA_HOME%\bin\java -Xms64m -Xmx96m -cp %FEDORA_HOME%\client;%FEDORA_HOME%\client\client.jar -Dfedora.home=%FEDORA_HOME% fedora.client.ingest.AutoIngestor %1 %2 %3 %4 %FEDORA_HOME%\demo\local-server-demos\formatting-objects-demo\obj-dbx-to-pdf.xml "Created by fedora-ingest-demos script"
%JAVA_HOME%\bin\java -Xms64m -Xmx96m -cp %FEDORA_HOME%\client;%FEDORA_HOME%\client\client.jar -Dfedora.home=%FEDORA_HOME% fedora.client.ingest.AutoIngestor %1 %2 %3 %4 %FEDORA_HOME%\demo\local-server-demos\formatting-objects-demo\obj-fop-to-pdf.xml "Created by fedora-ingest-demos script"
%JAVA_HOME%\bin\java -Xms64m -Xmx96m -cp %FEDORA_HOME%\client;%FEDORA_HOME%\client\client.jar -Dfedora.home=%FEDORA_HOME% fedora.client.ingest.AutoIngestor %1 %2 %3 %4 %FEDORA_HOME%\demo\local-server-demos\formatting-objects-demo\obj-tei-to-pdf.xml "Created by fedora-ingest-demos script"

echo Ingesting open-server simple image demos (2 bmechs, 2 objects)...
%JAVA_HOME%\bin\java -Xms64m -Xmx96m -cp %FEDORA_HOME%\client;%FEDORA_HOME%\client\client.jar -Dfedora.home=%FEDORA_HOME% fedora.client.ingest.AutoIngestor %1 %2 %3 %4 %FEDORA_HOME%\demo\open-server-demos\simple-image-demos\bmech-simple-image-4res-zoom.xml "Created by fedora-ingest-demos script"
%JAVA_HOME%\bin\java -Xms64m -Xmx96m -cp %FEDORA_HOME%\client;%FEDORA_HOME%\client\client.jar -Dfedora.home=%FEDORA_HOME% fedora.client.ingest.AutoIngestor %1 %2 %3 %4 %FEDORA_HOME%\demo\open-server-demos\simple-image-demos\bmech-simple-image-mrsid.xml "Created by fedora-ingest-demos script"
%JAVA_HOME%\bin\java -Xms64m -Xmx96m -cp %FEDORA_HOME%\client;%FEDORA_HOME%\client\client.jar -Dfedora.home=%FEDORA_HOME% fedora.client.ingest.AutoIngestor %1 %2 %3 %4 %FEDORA_HOME%\demo\open-server-demos\simple-image-demos\obj-image-4res-pavilliondraw.xml "Created by fedora-ingest-demos script"
%JAVA_HOME%\bin\java -Xms64m -Xmx96m -cp %FEDORA_HOME%\client;%FEDORA_HOME%\client\client.jar -Dfedora.home=%FEDORA_HOME% fedora.client.ingest.AutoIngestor %1 %2 %3 %4 %FEDORA_HOME%\demo\open-server-demos\simple-image-demos\obj-image-mrsid-pavillion.xml "Created by fedora-ingest-demos script"

echo Ingesting open-server user param image demo (1 bdef, 1 bmech, 2 objects)...
%JAVA_HOME%\bin\java -Xms64m -Xmx96m -cp %FEDORA_HOME%\client;%FEDORA_HOME%\client\client.jar -Dfedora.home=%FEDORA_HOME% fedora.client.ingest.AutoIngestor %1 %2 %3 %4 %FEDORA_HOME%\demo\open-server-demos\userinput-image-demo\bdef-image-userinput.xml "Created by fedora-ingest-demos script"
%JAVA_HOME%\bin\java -Xms64m -Xmx96m -cp %FEDORA_HOME%\client;%FEDORA_HOME%\client\client.jar -Dfedora.home=%FEDORA_HOME% fedora.client.ingest.AutoIngestor %1 %2 %3 %4 %FEDORA_HOME%\demo\open-server-demos\userinput-image-demo\bmech-image-userinput-mrsid.xml "Created by fedora-ingest-demos script"
%JAVA_HOME%\bin\java -Xms64m -Xmx96m -cp %FEDORA_HOME%\client;%FEDORA_HOME%\client\client.jar -Dfedora.home=%FEDORA_HOME% fedora.client.ingest.AutoIngestor %1 %2 %3 %4 %FEDORA_HOME%\demo\open-server-demos\userinput-image-demo\obj-image-userinput-archdraw.xml "Created by fedora-ingest-demos script"
%JAVA_HOME%\bin\java -Xms64m -Xmx96m -cp %FEDORA_HOME%\client;%FEDORA_HOME%\client\client.jar -Dfedora.home=%FEDORA_HOME% fedora.client.ingest.AutoIngestor %1 %2 %3 %4 %FEDORA_HOME%\demo\open-server-demos\userinput-image-demo\obj-image-userinput-column.xml "Created by fedora-ingest-demos script"

echo Ingesting open-server EAD finding aid demo (1 bdef, 1 bmech, 1 object)...
%JAVA_HOME%\bin\java -Xms64m -Xmx96m -cp %FEDORA_HOME%\client;%FEDORA_HOME%\client\client.jar -Dfedora.home=%FEDORA_HOME% fedora.client.ingest.AutoIngestor %1 %2 %3 %4 %FEDORA_HOME%\demo\open-server-demos\EAD-finding-aid-demo\bdef-ead-finding-aid.xml "Created by fedora-ingest-demos script"
%JAVA_HOME%\bin\java -Xms64m -Xmx96m -cp %FEDORA_HOME%\client;%FEDORA_HOME%\client\client.jar -Dfedora.home=%FEDORA_HOME% fedora.client.ingest.AutoIngestor %1 %2 %3 %4 %FEDORA_HOME%\demo\open-server-demos\EAD-finding-aid-demo\bmech-ead-finding-aid.xml "Created by fedora-ingest-demos script"
%JAVA_HOME%\bin\java -Xms64m -Xmx96m -cp %FEDORA_HOME%\client;%FEDORA_HOME%\client\client.jar -Dfedora.home=%FEDORA_HOME% fedora.client.ingest.AutoIngestor %1 %2 %3 %4 %FEDORA_HOME%\demo\open-server-demos\EAD-finding-aid-demo\obj-ead-finding-aid.xml "Created by fedora-ingest-demos script"

echo Finished.

set JAVA_HOME=%OLD_JAVA_HOME%

goto end

:checkEnv
if "%FEDORA_HOME%" == "" goto noFedoraHome
if not exist %FEDORA_HOME%\client\client.jar goto clientNotFound
if "%FEDORA_JAVA_HOME%" == "" goto tryJavaHome
set THIS_JAVA_HOME=%FEDORA_JAVA_HOME%
:checkJava
if not exist %THIS_JAVA_HOME%\bin\java.exe goto noJavaBin
if not exist %THIS_JAVA_HOME%\bin\orbd.exe goto badJavaVersion
goto envOk

:tryJavaHome
echo Warning: FEDORA_JAVA_HOME not set, falling back to JAVA_HOME
if "%JAVA_HOME%" == "" goto noJavaHome
set THIS_JAVA_HOME=%JAVA_HOME%
goto checkJava

:noFedoraHome
echo ERROR: Environment variable, FEDORA_HOME must be set.
goto end

:clientNotFound
echo ERROR: FEDORA_HOME does not appear correctly set.
echo Client cannot be found at %FEDORA_HOME%\client\client.jar
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

