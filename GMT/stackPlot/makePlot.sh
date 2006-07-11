#!/bin/sh

TYPELIST="data synth residual"

# if you change data....
# to get min val run
# grdinfo data.grd | grep z_max 
# and put 0/max value into grd2cpt -L option

rm -f grdcolor.cpt
grd2cpt ${type}.grd -Cwysiwyg -L0/0.0266749 > grdcolor.cpt

for type in $TYPELIST ; do
    OUT=${type}.ps
    xyz2grd ${type}.xyz -G${type}.grd -I.0025/.25 -H -R1.6/2.1/25/69.75
   grdimage ${type}.grd -P -JX3i/-3i -B.1/5 -R1.6/2.1/25/70 -Cgrdcolor.cpt > ${OUT}
    ps2pdf ${OUT}
done
