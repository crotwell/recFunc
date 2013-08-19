#!/bin/bash

AMP='&'

FILENAME=westUS.ps
rm -f $FILENAME
touch $FILENAME
pscoast -JM6i -R-130/-100/30/50 -Di -B5/5 -Na -W >> $FILENAME

LAT=30
while [ $LAT -lt 45 ]; do
   LATB=$[${LAT}+2]
   echo "$LAT $LATB"

   curl -o data${LAT}.txt http://www.seis.sc.edu/ears_tmp/stationLatLonBox.txt?minLat=${LAT}${AMP}maxLat=${LATB}${AMP}minLon=-130${AMP}maxLon=-100
   echo "curl -o data${LAT}.txt http://www.seis.sc.edu/ears_tmp/stationLatLonBox.txt?minLat=${LAT}${AMP}maxLat=${LATB}${AMP}minLon=-130${AMP}maxLon=-100"
   grep -v '\.\.\.' data${LAT}.txt | cleantxt.pl | perl -nae 'print "$F[3] $F[4]\n"' | psxy -JX6i/-6i  -R-130/-100/20/70 -Sc.1i -H1 -B5/5:."${LAT}-${LATB}": > westUS${LAT}.ps

   let LAT+=2
done

