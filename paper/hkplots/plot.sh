#!/bin/bash

AMP='&'
Q='"'

#NETID=62
#STA=Y12C
#SMALLESTH=22.5
#NETID=48
#STA=MM12
#SMALLESTH=25
NETID=7
STA=CCM
SMALLESTH=25

curl -o ${STA}.xyz http://www.seis.sc.edu/ears_tmp/sumHKStackAsXYZ.txt?netdbid=${NETID}${AMP}stacode=${STA}${AMP}minPercentMatch=80

xyz2grd ${STA}.xyz -G${STA}.grd -I.0025/.25 -R1.6/2.0975/${SMALLESTH}/69.75 -H

rm -f ${STA}.ps 
echo "grdcontour ${STA}.grd -C.05 -JX4.5/-4.5 -B.1/5 > ${STA}.ps"
grdcontour ${STA}.grd -C.05 -JX4.5/-4.5 -B.1:"Vp/Vs":/5:"Thickness (km)":WSen > ${STA}.ps

rm -f gray${STA}.cpt
MAX=`grdinfo -C ${STA}.grd | perl -nae '$a=$F[6]*1.3;print "$a"'`
echo "grd2cpt ${STA}.grd -I -Cgray -Z -L0/${MAX} > gray${STA}.cpt"
grd2cpt ${STA}.grd -I -Cgray -Z -L0/${MAX} > gray${STA}.cpt

rm -f ${STA}image.ps
grdimage ${STA}.grd -Cgray${STA}.cpt -JX4.5/-4.5 -B.1:"Vp/Vs":/5:"Thickness (km)":WSen > ${STA}image.ps
