#!/bin/bash

NAME=ristra
REGION=-111/-102.5/20/70

/bin/rm -f ${NAME}H.ps

touch ${NAME}H.ps
perl -nae 'print "$F[4] $F[5] $F[6] $F[6]\n"' ${NAME}.txt | psxy -K -P -H2 -JX6i/-8i -R${REGION} -B2:"Longitude":/5:"Thickness": -Sc.1i -G155 -Ey >> ${NAME}H.ps
perl -nae 'print "$F[4] $F[13]\n"' ${NAME}.txt | psxy -O -K -H2 -JX -R -Ss.1i -G0 >> ${NAME}H.ps

perl -nae 'print "$F[4] 30 9 90 1 ML $F[1]\n"' ${NAME}.txt | pstext  -O -H2 -JX -R -Y1.2i >> ${NAME}H.ps


