#!/bin/bash

. tf_env.sh

HARVEST_HOME=$FASCINATOR_HOME/code/core

function copy_sample {
	if [ ! -f $HARVEST_HOME/src/test/resources/$1 ]; then
		cp $HARVEST_HOME/src/test/resources/$1.sample $HARVEST_HOME/src/test/resources/$1
	fi
}

function copy_samples {
	copy_sample json-queue.json
	copy_sample local-files.json
	copy_sample local-files.py
	copy_sample index.json
	copy_sample usq.json
	copy_sample usq.py
}

if [ "$1" == "" ]; then
	echo "Usage: ./tf_index.sh <profile>"
	echo " Profiles: index-test"
else
	OS=`uname`
	if [ "$OS" == "Darwin" ]; then
		TEST=`ps a | grep "java -jar start.jar"`
	else
		TEST=`pgrep -l -f "java -jar start.jar"` 
	fi
	if [ $? ]; then
		copy_samples
		cd $FASCINATOR_HOME/code/core
		mvn -P $1 exec:java
		cd $OLDPWD
	else
		echo "[ERROR] SOLR does not appear to be running"
	fi
fi
