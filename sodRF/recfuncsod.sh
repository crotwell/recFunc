#!/bin/sh

MAVEN=/Users/crotwell/.maven/repository

export JACORB_HOME=/Users/crotwell/External/JacORB
export JACO=${JACORB_HOME}/bin/jaco

SEEDCODEC=$MAVEN/SeedCodec/jars/SeedCodec-1.0Beta.jar
FISSURESUTIL=$MAVEN/fissuresUtil/jars/fissuresUtil-1.0.6beta.jar
FISSURESIMPL=$MAVEN/fissuresImpl/jars/fissuresImpl-1.1.4beta.jar
FISSURESIDL=$MAVEN/fissuresIDL/jars/fissuresIDL-1.0.jar
LOG4J=$MAVEN/log4j/jars/log4j-1.2.8.jar
TAUP=$MAVEN/TauP/jars/TauP-1.1.4.jar
XALAN=$MAVEN/xalan/jars/xalan-2.5.1.jar
XERCES=$MAVEN/xerces/jars/xerces-2.4.0.jar
XMLAPI=$MAVEN/xml-apis/jars/xml-apis-1.0.b2.jar
JAICORE=$MAVEN/jars/jai_core.jar
JAICODEC=$MAVEN/jars/jai_codec.jar
HSQLDB=$MAVEN/hsqldb/jars/hsqldb-20040212.jar
GEE=$MAVEN/gee/jars/gee-2.0.4.jar
JING=$MAVEN/jing/jars/jing-20030619.jar
OPENMAP=$MAVEN/openmap/jars/openmap-4.5.4_USC1.jar
SOD=$MAVEN/sod/jars/sod-1.0Beta.jar
RECFUNC=$MAVEN/recFunc/jars/recFunc-1.0beta.jar

$JACO -Djacorb.connection.client.pending_reply_timeout=120000 -Xmx256m -cp ${SEEDCODEC}:${SOD}:${RECFUNC}:${OPENMAP}:${GEE}:${JING}:${FISSURESIDL}:${FISSURESIMPL}:${FISSURESUTIL}:${XERCES}:${XMLAPI}:${XALAN}:${TAUP}:${LOG4J}:${HSQLDB}:${CLASSPATH} edu.sc.seis.sod.Start $*

