#!/bin/sh

MAVEN=~/.maven/repository

JACORB=$MAVEN/JacORB/jars/JacORB-1.4.1.jar
# for orbacus
OB=$MAVEN/OB/jars/OB-4.1.0.jar
OBNAMING=$MAVEN/OBNaming/jars/OBNaming-4.1.0.jar

SEEDCODEC=$MAVEN/SeedCodec/jars/SeedCodec-1.0Beta.jar
FISSURESUTIL=$MAVEN/fissuresUtil/jars/fissuresUtil-1.0Beta.jar
FISSURESIMPL=$MAVEN/fissuresImpl/jars/fissuresImpl-1.1Beta.jar
FISSURESIDL=$MAVEN/fissuresIDL/jars/fissuresIDL-1.0.jar
GEOTOOLS=$MAVEN/Geotools1/jars/geotools1.2.jar
LOG4J=$MAVEN/log4j/jars/log4j-1.2.6.jar
TAUP=$MAVEN/TauP/jars/TauP-SNAPSHOT.jar
XALAN=$MAVEN/xalan/jars/xalan-2.4.1.jar
XERCES=$MAVEN/xerces/jars/xerces-2.2.0.jar
XMLAPI=$MAVEN/xml-apis/jars/xml-apis-1.0.b2.jar
JAICORE=$MAVEN/jars/jai_core.jar
JAICODEC=$MAVEN/jars/jai_codec.jar
HSQLDB=$MAVEN/hsqldb/jars/hsqldb-1.7.1.jar
SOD=$MAVEN/sod/jars/sod-1.0Beta.jar
GEE=$MAVEN/gee/jars/gee-1.1Beta.jar
RECFUNC=$MAVEN/recFunc/jars/recFunc-1.0Beta.jar


java -cp ${SEEDCODEC}:${SOD}:${RECFUNC}:${GEE}:${FISSURESIDL}:${FISSURESIMPL}:${FISSURESUTIL}:${OB}:${OBNAMING}:${XERCES}:${XMLAPI}:${XALAN}:${TAUP}:${LOG4J}:${HSQLDB}:${CLASSPATH} edu.sc.seis.sod.Start $*

