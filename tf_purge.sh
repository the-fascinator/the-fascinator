#!/bin/bash
#
# this script is used for deleting fascinator data
# data will be deleted:
# 1. $TF_HOME/storage
# 2. $TF_HOME/activemq-data
# 3. $TF_HOME/logs
# 4. $TF_HOME/cache
# 5. $SOLR_BASE_DIR/solr/indexes/anotar/index
# 6. $SOLR_BASE_DIR/solr/indexes/fascinator/index
# 7. $SOLR_BASE_DIR/solr/indexes/security/index

# suppress console output from pushd/popd
pushd() {
	builtin pushd "$@" > /dev/null
}
popd() {
	builtin popd "$@" > /dev/null
}

# get fascinator home dir
pushd `dirname $0`
TF_CODE=`pwd`
popd

if [ "$1" == "" ]; then
	echo "Usage: ./`basename $0` all|solr"
	exit 0
fi

# setup environment
. $TF_CODE/tf_env.sh

if [ "$1" == "all" ]; then
    echo Deleting all data
    echo Deleting: $TF_HOME/storage
    rm -rf $TF_HOME/storage

    echo Deleting: $TF_HOME/activemq-data
    rm -rf $TF_HOME/activemq-data

    echo Deleting: $TF_HOME/logs
    rm -rf $TF_HOME/logs

    echo Deleting: $TF_HOME/database/fsHarvestCache
    rm -rf $TF_HOME/database/fsHarvestCache

    echo Deleting: $TF_HOME/cache
    rm -rf $TF_HOME/cache
fi

if [ "$1" == "all" -o "$1" == "solr" ]; then
    echo Deleting solr data

    if [ ! -d "$SOLR_BASE_DIR/solr" ]; then
       SOLR_BASE_DIR = $TF_HOME
    fi
    
    echo Deleting: $SOLR_BASE_DIR/solr/indexes/anotar/index
    rm -rf $SOLR_BASE_DIR/solr/indexes/anotar/index

    echo Deleting: $SOLR_BASE_DIR/solr/indexes/fascinator/index
    rm -rf $SOLR_BASE_DIR/solr/indexes/fascinator/index

    echo Deleting: $SOLR_BASE_DIR/solr/indexes/security/index
    rm -rf $SOLR_BASE_DIR/solr/indexes/security/index
fi

