#!/bin/sh

# Cannot proceed if FEDORA_HOME is not set
if [ -z "$FEDORA_HOME" ]; then
	echo "ERROR: The FEDORA_HOME environment variable is not defined."
	exit 1
fi

if [ -r "$FEDORA_HOME"/server/bin/set-env.sh ]; then
  	. "$FEDORA_HOME"/server/bin/set-env.sh
else
	echo "ERROR: $FEDORA_HOME/server/bin/set-env.sh was not found."
	exit 1
fi

TOMCAT_DIR=@tomcat.basename@
WEBAPP_DIR="$FEDORA_HOME/server/$TOMCAT_DIR/webapps/fedora/WEB-INF"

(exec "$JAVA" -Dfedora.home="$FEDORA_HOME" \
        -cp "$WEBAPP_DIR/classes" \
        fedora.server.config.ConfigApp $1)

restoreJavaHome
