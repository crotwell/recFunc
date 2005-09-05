#!/bin/bash

OUT=stationMap.ps

/bin/rm -f $OUT

touch $OUT

psbasemap  -K -P -JN0/5i -R-180/180/-90/90  -Bg30/g15 >> $OUT
pscoast  -K -O -JN -R  -S210  -Di -A10000 -Bg30/g15 >> $OUT
perl -nae 'print "$F[3] $F[2]\n"' ears_latlon.txt | psxy -O -JN -R -St.07i -G5 >> $OUT


