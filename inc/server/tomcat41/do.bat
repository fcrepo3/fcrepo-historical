set BASE=c:\tomcat41min
set CLASSPATH=%JAVA_HOME%\lib\tools.jar;%BASE%\bin\bootstrap.jar
java -Dclasspath=%CLASSPATH% -Djava.endorsed.dirs=%BASE%\bin -Djava.security.manager -Djava.security.policy=%BASE%\conf\catalina.policy -Dcatalina.base=%BASE% -Dcatalina.home=%BASE% -Djava.io.tmpdir=%BASE%\temp org.apache.catalina.startup.Bootstrap start
