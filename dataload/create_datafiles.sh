#!/usr/bin/env bash

if [ $# == 0 ]; then
    echo "Usage: $0 <configurl> <outdir> [--loadLocalFiles]"
    exit 1
fi

SCRIPT_PATH=$(dirname $(readlink -f $0))

CONFIG_URL=$1
OUTDIR=$2

JSON_PATH=$OUTDIR/ontologies.json

rm -f $OUTDIR/*

echo owl2json
java -DentityExpansionLimit=0 -DtotalEntitySizeLimit=0 -Djdk.xml.totalEntitySizeLimit=0 -Djdk.xml.entityExpansionLimit=0 -jar $SCRIPT_PATH/owl2json/target/owl2json-1.0-SNAPSHOT.jar --config "$CONFIG_URL" --output "$JSON_PATH" "${@:3}"

echo json2neo
java -jar $SCRIPT_PATH/json2neo/target/json2neo-1.0-SNAPSHOT.jar --input "$JSON_PATH" --outDir $OUTDIR

echo json2solr
java -jar $SCRIPT_PATH/json2solr/target/json2solr-1.0-SNAPSHOT.jar --input "$JSON_PATH" --outDir $OUTDIR


