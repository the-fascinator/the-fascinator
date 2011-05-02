#!/bin/bash
#
# this script starts a fascinator harvest using maven
# only usable when installed in development mode
#
# get absolute path of where the script is run from
PROG_DIR=`cd \`dirname $0\`; pwd`

# file to store pid
PID_FILE=$PROG_DIR/tf.pid

# display program header
echo "The Fascinator (Developer) - Harvest Client"

# setup environment
. $PROG_DIR/tf_env.sh

# harvest config directory
HARVEST_DIR=$TF_HOME/harvest

usage() {
	echo "Usage: `basename $0` JSON_FILE"
	echo "Where JSON_FILE is a JSON configuration file."
	echo "If JSON_FILE is not an absolute path, the file is assumed to be in:"
	echo "    $HARVEST_DIR"
	echo "Available sample files:"
	for HARVEST_FILE in `ls $HARVEST_DIR/*.json`; do
		_TMP=${HARVEST_FILE##*/harvest/}
		echo -n "    "
		echo $_TMP | cut -d . -f 1-1
	done
	exit 1
}

running() {
	[ -f $PID_FILE ] || return 1
	PID=$(cat $PID_FILE)
	ps -p $PID > /dev/null 2> /dev/null || return 1
	return 0
}

# check script arguments
[ $# -gt 0 ] || usage

# only run harvest if fascinator is running
if running; then
	if [ -f $1 ]; then
		JSON_FILE=$1
	else
		JSON_FILE=$HARVEST_DIR/$1.json
	fi
	shift
	ARGS="$*"
	echo " * Starting harvest with: $JSON_FILE"
	if [ -f $JSON_FILE ]; then
		mvn $ARGS \
			-f $PROG_DIR/core/pom.xml \
			-P dev \
			-Dexec.args=$JSON_FILE \
			-Dexec.mainClass=au.edu.usq.fascinator.HarvestClient \
			exec:java &> $TF_HOME/logs/harvest.out
		echo "   - Finished on `date`"
		echo "   - Log file: $TF_HOME/logs/harvest.out"
	else
		echo "   - File not found!"
		usage
	fi
else
	echo " * The Fascinator is not running!"
fi
