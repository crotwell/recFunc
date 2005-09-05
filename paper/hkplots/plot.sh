#!/bin/bash

AMP='&'

NETID=62
STA=Y12C

curl -o ${STA}.xyz http://www.seis.sc.edu/ears_tmp/sumHKStackAsXYZ.txt?netdbid=${NETID}${AMP}stacode=${STA}${AMP}minPercentMatch=80

xyz2grd ${STA}.xyz -G${STA}.grd -I.0025/.25 -R1.6/2.0975/22.5/69.75 -H

rm -f ${STA}.ps 
grdcontour ${STA}.grd -C.05 -JX6/-6 -B.1/5 > ${STA}.ps

rm -f gray${STA}.cpt
MAX=`grdinfo -C Y12C.grd | perl -nae '$a=$F[6]*1.5;print "$a"'`
echo "grd2cpt ${STA}.grd -I -Cgray -Z -L0/${MAX} > gray${STA}.cpt"
grd2cpt ${STA}.grd -I -Cgray -Z -L0/${MAX} > gray${STA}.cpt

rm -f ${STA}image.ps
echo grdimage ${STA}.grd -Cgray${STA}.cpt -JX6/-6 -B.1/5 > ${STA}image.ps
grdimage ${STA}.grd -Cgray${STA}.cpt -JX6/-6 -B.1/5 > ${STA}image.ps
