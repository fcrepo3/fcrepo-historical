#!/bin/sh
# ----------------------------------------------------------------------
# Fedora Server stop script
# ----------------------------------------------------------------------
# Cannot proceed if FEDORA_HOME is not set
if [ -z "$FEDORA_HOME" ]; then
	echo "ERROR: The FEDORA_HOME environment variable is not defined."
	exit 1
fi

if [ -r "$FEDORA_HOME"/server/bin/fedora.sh ]; then
  	. "$FEDORA_HOME"/server/bin/fedora.sh
else
	echo "ERROR: $FEDORA_HOME/server/bin/fedora.sh was not found."
	exit 1
fi

exec "$FEDORA_HOME"/server/bin/fedora.sh stop "$@"
