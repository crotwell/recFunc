#!/bin/sh

rm -f ears_crust2_H.ps ears_histo.ps

#
echo histogram of number of stacks used
#
perl -nae 'print "$F[6]\n"' depth_vpvs.txt | pshistogram -JXh -W20 -L1g -V > ears_histo.ps
pstopdf ears_histo.ps 

#
echo EARS vs Crust2 for all
#
perl -nae 'print "$F[7] $F[3]\n"' depth_vpvs.txt | psxy -K -JX6i -R0/80/0/80 -Sc.1 -B10:"Crust2.0 Thickness":/:"EARS Thickness"::."EARS vs Crust2.0":WeSn > ears_crust2_H.ps
psxy -O -JX -R >> ears_crust2_H.ps <<END
0 0
100 100
END
pstopdf ears_crust2_H.ps

#
echo EARS vs Crust2 for IU/II/IC
#
grep 'I[ICU]' depth_vpvs.txt | perl -nae 'print "$F[7] $F[3]\n"' - | psxy -K -JX6i -R0/80/0/80 -Sc.1 -B10:"Crust2.0 Thickness":/:"EARS Thickness"::."EARS vs Crust2.0 for GSN":WeSn > GSNears_crust2_H.ps
psxy -O -JX -R >> GSNears_crust2_H.ps <<END
0 0
100 100
END
pstopdf GSNears_crust2_H.ps

#
echo  EARS vs Crust2 for IU/II/IC Vp/Vs
#
grep 'I[ICU]' depth_vpvs.txt | perl -nae 'print "$F[8] $F[4]\n"' - | psxy -K -JX6i -R1.5/2.2/1.5/2.2 -Sc.1 -B.1:"Crust2.0 VpVs":/:"EARS VpVs"::."EARS vs Crust2.0 for GSN":WeSn > GSNears_crust2_vpvs.ps
psxy -O -JX -R >> GSNears_crust2_vpvs.ps <<END
0 0
100 100
END
pstopdf GSNears_crust2_vpvs.ps

#
echo  EARS vs Crust2 for XM99
#
grep 'XM' depth_vpvs.txt | perl -nae 'print "$F[7] $F[3]\n"' - | psxy -K -JX6i -R0/80/0/80 -Sc.1 -B10:"Crust2.0 Thickness":/:"EARS Thickness"::."EARS vs Crust2.0":WeSn > XM99ears_crust2_H.ps
psxy -O -JX -R >> XM99ears_crust2_H.ps <<END
0 0
100 100
END
pstopdf XM99ears_crust2_H.ps

#
echo EARS vs David Wilson for XM99
#
python compareXM99.py | perl -nae 'print "$F[0] $F[1]\n"' | psxy -K -H1 -JX6i -R20/70/20/70 -Sc.1 -B10:"Wilson Thickness":/:"EARS Thickness"::."EARS vs D. Wilson":WeSn > XM99ears_wilson_H.ps
psxy -O -JX -R >> XM99ears_wilson_H.ps <<END
0 0
100 100
END
pstopdf XM99ears_wilson_H.ps

#
echo EARS and Wilson vs lon for XM99
#
grep 'XM99' depth_vpvs.txt | perl -nae 'print "$F[2] $F[3]\n"' - | psxy -K -JX6i -R-112/-101/0/80 -Sc.2 -G0 -B1:"Longitude":/10:"Thickness"::."EARS vs Wilson":WeSn > XM99ears_wilson_lon_H.ps
perl -nae 'print "$F[2] $F[5]\n"' ../DavidWilson/ristrastationvels.txt | psxy -O -JX -R -Sd.2   >> XM99ears_wilson_lon_H.ps
pstopdf XM99ears_wilson_lon_H.ps

#
echo Ears H california map
#
psbasemap -K  -X1i -Y1i -JM6i -R-125/-115/32/44 -P  -B2 -U  > california.ps
grdimage crust2_180.grd -Credblue.cpt -JM -R -K -O >> california.ps
pscoast -K -O -A10000 -JM -R   -W -Di -N2 -N1  >> california.ps
# for diff to crust2
#cat depth_vpvs.txt |  perl -nae '$d = $F[3]-$F[7];print "$F[2] $F[1] $d\n"' - | psxy -K -O -JM -R -Cdiffcrust2.cpt -St0.35 >> california.ps
# for thickness
cat depth_vpvs.txt |  perl -nae '$d = $F[3];print "$F[2] $F[1] $d\n"' - | psxy -K -O -JM -R -Credblue.cpt -St0.35 >> california.ps

pstext -O -JM -R >> california.ps <<END
0 0 12 0 0 1 .
END
pstopdf california.ps
echo done california

#
echo Ears H usa map
#
psbasemap -K  -X1i -Y1i -JM10i -R-125/-65/22/50  -B10 -U  > usa_H.ps
#grdimage crust2_180.grd -Credblue.cpt -JM -R -K -O >> usa_H.ps
pscoast -K -O -A10000 -JM -R   -W -Di -N2 -N1  >> usa_H.ps
cat depth_vpvs.txt | grep US |  perl -nae '$d = $F[3]-$F[7];print "$F[2] $F[1] $d\n"' - | psxy -K -O -JM -R -Cdiffcrust2.cpt -St0.35 >> usa_H.ps

pstext -O -JM -R >> usa_H.ps <<END
0 0 12 0 0 1 .
END
pstopdf usa_H.ps
echo done usa

#
echo Ears H map GSN
#
psbasemap -K  -X1i -Y1i -JQ0/10i -R-180/180/-90/90  -B20 -U  > gsn_world_H.ps
grdimage crust2_180.grd -Credblue.cpt -JQ -R -K -O >> gsn_world_H.ps
pscoast -K -O -A10000 -JQ -R   -W -Di    >> gsn_world_H.ps
cat depth_vpvs.txt | grep 'I[ICU]' | perl -nae '$d = $F[3];print "$F[2] $F[1] $d\n"' - | psxy -K -O -JQ -R -Credblue.cpt -St0.35 >> gsn_world_H.ps
cat depth_vpvs.txt | grep 'I[ICU]' | perl -nae '$d = $F[3];print "$F[2] $F[1] $d\n"' - | psxy -K -O -JQ -R  -St0.35 >> gsn_world_H.ps
#cat depth_vpvs.txt  | perl -nae '$d = $F[3]-$F[7];print "$F[2] $F[1] $d\n"' - | psxy -K -O -JQ -R -Cdiffcrust2.cpt -St0.35 >> gsn_world_H.ps

pstext -O -JQ -R >> gsn_world_H.ps <<END
0 0 12 0 0 1 .
END
pstopdf gsn_world_H.ps


#
echo Ears H map
#
psbasemap -K  -X1i -Y1i -JQ0/10i -R-180/180/-90/90  -B20 -U  > world_H.ps
grdimage crust2_180.grd -Credblue.cpt -JQ -R -K -O >> world_H.ps
pscoast -K -O -A10000 -JQ -R   -W -Di    >> world_H.ps
cat depth_vpvs.txt |  perl -nae '$d = $F[3];print "$F[2] $F[1] $d\n"' - | psxy -K -O -JQ -R -Credblue.cpt -St0.35 >> world_H.ps
cat depth_vpvs.txt |  perl -nae '$d = $F[3];print "$F[2] $F[1] $d\n"' - | psxy -K -O -JQ -R  -St0.35 >> world_H.ps
#cat depth_vpvs.txt  | perl -nae '$d = $F[3]-$F[7];print "$F[2] $F[1] $d\n"' - | psxy -K -O -JQ -R -Cdiffcrust2.cpt -St0.35 >> world_H.ps

pstext -O -JQ -R >> world_H.ps <<END
0 0 12 0 0 1 .
END
pstopdf world_H.ps

#
echo ears vs Lupei for S Cal
#
python compareZhu.py | perl -nae 'print "$F[0] $F[1] $F[2] $F[3]\n"' | psxy -K -H1 -JX6i -R20/50/20/50 -Sc.1 -B10:"Zhu Thickness":/:"EARS Thickness"::."EARS vs L. Zhu":WeSn -G0 -Exy > ears_zhu_H.ps
psxy -O -JX -R >> ears_zhu_H.ps <<END
0 0
100 100
END
pstopdf ears_zhu_H.ps

