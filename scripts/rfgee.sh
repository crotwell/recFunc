#!/bin/sh
JACORB=$MAVEN_HOME/repository/JacORB/jars/JacORB-1.4.1.jar
# for orbacus
OB=$MAVEN_HOME/repository/OB/jars/OB-4.1.0.jar
OBNAMING=$MAVEN_HOME/repository/OBNaming/jars/OBNaming-4.1.0.jar

HSQLDB=$MAVEN_HOME/repository/hsqldb/jars/hsqldb-1.7.1.jar

SEEDCODEC=$MAVEN_HOME/repository/SeedCodec/jars/SeedCodec-1.0Beta.jar
FISSURESUTIL=$MAVEN_HOME/repository/fissuresUtil/jars/fissuresUtil-1.0Beta.jar
FISSURESIMPL=$MAVEN_HOME/repository/fissuresImpl/jars/fissuresImpl-1.1Beta.jar
FISSURESIDL=$MAVEN_HOME/repository/fissuresIDL/jars/fissuresIDL-1.0.jar
GEOTOOLS=$MAVEN_HOME/repository/Geotools1/jars/geotools1.2.jar
GT2CORE=$MAVEN_HOME/repository/gt2/jars/core-0.1.jar
GT2DCORE=$MAVEN_HOME/repository/gt2/jars/defaultcore-0.1.jar
GT2GUI=$MAVEN_HOME/repository/gt2/jars/gui-0.1.jar
GT2REND=$MAVEN_HOME/repository/gt2/jars/java2drendering-0.1.jar
GT2SHP=$MAVEN_HOME/repository/gt2/jars/shapefile-0.1.jar
GT2CTS=$MAVEN_HOME/repository/gt2/jars/cts-coordtrans-0.6.jar
GT2RESOURCE=$MAVEN_HOME/repository/gt2/jars/resources-0.1.jar
JTS=$MAVEN_HOME/repository/JTS/jars/JTS-1.3.jar
OPENGIS=$MAVEN_HOME/repository/opengis/jars/opengis-css-0.1.jar
LOG4J=$MAVEN_HOME/repository/log4j/jars/log4j-1.2.6.jar
TAUP=$MAVEN_HOME/repository/TauP/jars/TauP-1.1.4.jar
XALAN=$MAVEN_HOME/repository/xalan/jars/xalan-2.4.1.jar
XERCES=$MAVEN_HOME/repository/xerces/jars/xerces-2.0.2.jar
XMLAPI=$MAVEN_HOME/repository/xml-apis/jars/xml-apis-1.0.b2.jar
JAICORE=$MAVEN_HOME/repository/jars/jai_core.jar
JAICODEC=$MAVEN_HOME/repository/jars/jai_codec.jar
CLASSICS=$MAVEN_HOME/repository/classics/jars/classics-1.0.jar
GEEMAC=$MAVEN_HOME/repository/geeMac/jars/geeMac-1.1Beta.jar
RECFUNC=$MAVEN_HOME/repository/recFunc/jars/recFunc-1.0Beta.jar
GEE=$MAVEN_HOME/repository/gee/jars//gee-1.1Beta.jar

NATIVEFFTDIR=../native

# use orbacus
java -Djava.library.path=$NATIVEFFTDIR -cp ${RECFUNC}:${GEE}:${GT2SHP}:${GEEMAC}:${CLASSICS}:${HSQLDB}:${XMLAPI}:${XERCES}:${XALAN}:${TAUP}:${LOG4J}:${SEEDCODEC}:${FISSURESIDL}:${FISSURESIMPL}:${FISSURESUTIL}:${GEOTOOLS}:${GT2CORE}:${GT2DCORE}:${GT2GUI}:${GT2REND}:${GT2CTS}:${GT2RESOURCE}:${JTS}:${OPENGIS}:${OB}:${OBNAMING}:${JAICORE}:${JAICODEC}:${CLASSPATH} edu.sc.seis.vsnexplorer.Start -props ./recfunc.prop

echo done.

