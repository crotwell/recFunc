#!/bin/sh

rm -f ears_crust2_H.ps ears_histo.ps

#
# histogram of number of stacks used
#
perl -nae 'print "$F[6]\n"' depth_vpvs.txt | pshistogram -JXh -W20 -L1g -V > ears_histo.ps

#
# EARS vs Crust2 for all
#
perl -nae 'print "$F[7] $F[3]\n"' depth_vpvs.txt | psxy -K -JX6i -R0/80/0/80 -Sc.1 -B10:"Crust2.0 Thickness":/:"EARS Thickness"::."EARS vs Crust2.0":WeSn > ears_crust2_H.ps
psxy -O -JX -R >> ears_crust2_H.ps <<END
0 0
100 100
END

#
# EARS vs Crust2 for XM99
#
grep 'XM' depth_vpvs.txt | perl -nae 'print "$F[7] $F[3]\n"' - | psxy -K -JX6i -R0/80/0/80 -Sc.1 -B10:"Crust2.0 Thickness":/:"EARS Thickness"::."EARS vs Crust2.0":WeSn > XM99ears_crust2_H.ps
psxy -O -JX -R >> XM99ears_crust2_H.ps <<END
0 0
100 100
END

#
# EARS vs David Wilson for XM99
#
python compareXM99.py | perl -nae 'print "$F[0] $F[1]\n"' | psxy -K -H1 -JX6i -R20/70/20/70 -Sc.1 -B10:"Wilson Thickness":/:"EARS Thickness"::."EARS vs D. Wilson":WeSn > XM99ears_wilson_H.ps
psxy -O -JX -R >> XM99ears_wilson_H.ps <<END
0 0
100 100
END

#
# EARS and Wilson vs lon for XM99
#
grep 'XM99' depth_vpvs.txt | perl -nae 'print "$F[2] $F[3]\n"' - | psxy -K -JX6i -R-112/-101/0/80 -Sc.2 -B1:"Longitude":/10:"Thickness"::."EARS vs Wilson":WeSn > XM99ears_wilson_lon_H.ps
perl -nae 'print "$F[2] $F[5]\n"' ../DavidWilson/ristrastationvels.txt | psxy -O -JX -R -Sd.2 -G1  >> XM99ears_wilson_lon_H.ps

