#!/bin/bash

OUT=stationMap.ps

/bin/rm -f $OUT

touch $OUT

psbasemap  -K -JN0/9 -R-180/180/-90/90  -Ba30g30/a30g15 >> $OUT
pscoast  -K -O -JN -R  -S210  -Di -A10000 -Ba30g30/a30g15 >> $OUT
perl -nae 'print "$F[3] $F[2]\n"' ears_latlon.txt | psxy -O -JN -R -St.07 -G5 >> $OUT


