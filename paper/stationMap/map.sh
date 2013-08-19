#!/bin/bash

gmtset PLOT_DEGREE_FORMAT -D ANNOT_FONT_PRIMARY Times-Roman

OUT=stationMap.ps

/bin/rm -f $OUT

touch $OUT

psbasemap  -K -P -JN0/5i -R-180/180/-90/90  -Bg30/g15 >> $OUT
pscoast  -K -O -JN -R  -S210  -Dl -A10000 -Bg30/g15 >> $OUT
perl -nae 'print "$F[3] $F[2]\n"' ears_latlon.txt | psxy -O -K -JN -R -St.07i -G5 >> $OUT


psbasemap  -O -K -Y3 -P -JM2.5i -R-122/-113/32/37  -Ba2/a4f2WeNs >> $OUT
pscoast  -K -O -JM -R  -S210  -Dl -N2 -A10000  >> $OUT
perl -nae 'print "$F[4] $F[3]\n"' ../lupei/lupei.txt | psxy -H2 -O -K -JM -R -St.07i -G5 >> $OUT

psbasemap  -O -K -X3 -P -JM2.5i -R-94/-70/35/45  -Ba5/a4f2wENs >> $OUT
pscoast  -K -O -JM -R  -S210  -Dl -N2 -A10000  >> $OUT
perl -nae 'print "$F[4] $F[3]\n"' ../moma/moma.txt | psxy -H2 -O -JM -R -St.07i -G5 >> $OUT
