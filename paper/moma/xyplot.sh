#!/bin/bash

NAME=moma
REGION=-93/-70.5/20/70

/bin/rm -f ${NAME}H.ps

touch ${NAME}H.ps
perl -nae 'print "$F[4] $F[5] $F[6] $F[6]\n"' ${NAME}.txt | psxy -K -P -H2 -JX6/-8 -R${REGION} -B2/2 -Sc.1 -G255/0/0 -Ey >> ${NAME}H.ps
perl -nae 'print "$F[4] $F[5] 9 0 1 ML .$F[1]\n"' ${NAME}.txt | pstext  -O -K -H2 -JX -R >> ${NAME}H.ps

perl -nae 'print "$F[4] $F[13]\n"' ${NAME}.txt | psxy -O -H2 -JX -R -Ss.1 -G0/255/0 >> ${NAME}H.ps


