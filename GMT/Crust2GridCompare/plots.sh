#!/bin/bash

# PE 6.35 3.61 1.76 50 38.75 1.75 -113.0 47.0

OUT=map
CRUST2_DATA=crust2GridCompare_1.txt
rm -f ${OUT}.ps 
pscoast -K -JH0/9i -R-180/180/-80/80 -B30 -A1000 -Di -W >> ${OUT}.ps

perl -nae '$d=$F[4]-$F[5];$a=$F[7]-1;$b=$F[8]-1;print "> -Z$d\n";print "$a $b\n";$a+=2;print "$a $b\n";$b+=2;print "$a $b\n";$a-=2;print "$a $b\n"' \
   $CRUST2_DATA | psxy -O -JH -R -M -L  -Credblue.cpt >> ${OUT}.ps
#ps2pdf ${OUT}.ps
#rm -f ${OUT}.ps
#open ${OUT}.pdf
