#!/bin/bash

# Net Station Name Lat Lon Est.Thick StdDev Est.Vp/Vs StdDev AssumedVp Vs PoissonsRatio NumEQ Complexity Thick Vp/Vs Vp Vs PoissonsRatio
# 0       1     2   3   4       5        6      7        8       9     10      11         12       13      14    15  16 17    18


#
echo histogram of number of eq used
#
OUT=eq_histo
perl -nae 'print "$F[12]\n"' depth_vpvs.txt | pshistogram -JXh -W20 -L1g -V > ${OUT}.ps
pstopdf ${OUT}.ps 
rm ${OUT}.ps 

#
echo histogram of diff to crust2.0
#
OUT=crust2_histo
perl -nae '$a=$F[14]-$F[5];print "$a\n"' depth_vpvs.txt | pshistogram -JXh -W5 -L1g -V -B:."Diff to Crust2 Histogram": > ${OUT}.ps
pstopdf ${OUT}.ps 
rm ${OUT}.ps 


#
echo histogram of complexity
#
OUT=complexity_histo
perl -nae 'print "$F[13]\n"'  depth_vpvs.txt | pshistogram -JXh -W0.1 -L1g -V > ${OUT}.ps
pstopdf ${OUT}.ps 
rm ${OUT}.ps 


echo num eq vs complexity
OUT=numeqVsComplexity
psbasemap -JX6i/6il -R0/1/1/1000 -K  -B.1:"Complexity":/100:"Num EQ"::."NumEQ vs. Complexity": > ${OUT}.ps
pstopdf ${OUT}.ps
rm ${OUT}.ps

echo EARS VpVs vs complexity for all
#
OUT=earsVpVs_vs_complexity
perl -nae 'print "$F[13] $F[7]\n"' depth_vpvs.txt | psxy -K -JX6i -R0/1/1.6/2.1 -Sc.1 -B.10:"Complexity":/.1:"Vp/Vs"::."EARS Vp/Vs vs Complexity":WeSn > ${OUT}.ps
psxy -O -JX -R >> ${OUT}.ps <<END
0 0
100 100
END
pstopdf ${OUT}.ps
rm ${OUT}.ps

echo EARS - Crust2 vs complexity for all
#
OUT=ears-crust2_vs_complexity_H
touch ${OUT}.ps
perl -nae 'if ($F[7] > 1.7 && $F[7] < 1.85 ) {$a=$F[14]-$F[5]; print "$F[13] $a\n";}' depth_vpvs.txt | psxy -K -JX6i -R0/1/-80/80 -Sc.1 -G0/255/0 -B.10:"Complexity":/10:"Crust2.0 Thickness - EARS Thickness"::."EARS - Crust2.0 vs Complexity":WeSn >> ${OUT}.ps
perl -nae 'if ($F[7] <= 1.7 ) {$a=$F[14]-$F[5]; print "$F[13] $a\n";}' depth_vpvs.txt | psxy -O -K -JX6i -R0/1/-80/80 -Sc.1 -G255/0/0 -B.10:"Complexity":/10:"Crust2.0 Thickness - EARS Thickness"::."EARS - Crust2.0 vs Complexity":WeSn >> ${OUT}.ps
perl -nae 'if ($F[7] >= 1.85 ) {$a=$F[14]-$F[5]; print "$F[13] $a\n";}' depth_vpvs.txt | psxy -O -K -JX6i -R0/1/-80/80 -Sc.1 -G0/0/255 -B.10:"Complexity":/10:"Crust2.0 Thickness - EARS Thickness"::."EARS - Crust2.0 vs Complexity":WeSn >> ${OUT}.ps
psxy -O -JX -R >> ${OUT}.ps <<END
0 0
100 100
END
pstopdf ${OUT}.ps
rm ${OUT}.ps

echo EARS - Crust2 vs bootstrap h error for all
#
OUT=ears-crust2_vs_bootstrap_h
touch ${OUT}.ps
perl -nae 'if ($F[7] > 1.7 && $F[7] < 1.85 ) {$a=$F[14]-$F[5]; print "$F[6] $a\n";}' depth_vpvs.txt | psxy -K -JX6i -R0/18/-80/80 -Sc.1 -G0/255/0 -B1.0:"Bootstrap H":/10:"Crust2.0 Thickness - EARS Thickness"::."EARS - Crust2.0 vs Bootstrap H":WeSn >> ${OUT}.ps
perl -nae 'if ($F[7] <= 1.7 ) {$a=$F[14]-$F[5]; print "$F[6] $a\n";}' depth_vpvs.txt | psxy -O -K -JX6i -R -Sc.1 -G255/0/0  >> ${OUT}.ps
perl -nae 'if ($F[7] >= 1.85 ) {$a=$F[14]-$F[5]; print "$F[6] $a\n";}' depth_vpvs.txt | psxy -O -K -JX6i -R -Sc.1 -G0/0/255  >> ${OUT}.ps
psxy -O -JX -R >> ${OUT}.ps <<END
0 0
100 0
END
pstopdf ${OUT}.ps
rm ${OUT}.ps

echo Complexity vs bootstrap h error for all
#
OUT=complexity_vs_bootstrap_h
touch ${OUT}.ps
perl -nae 'if ($F[7] > 1.7 && $F[7] < 1.85 ) {print "$F[6] $F[13]\n";}' depth_vpvs.txt | psxy -K -JX6i -R0/18/0/1 -Sc.1 -G0/255/0 -B1.0:"Bootstrap H":/.1:"Complexity"::."Complexity vs Bootstrap H":WeSn >> ${OUT}.ps
perl -nae 'if ($F[7] <= 1.7 ) {$a=$F[14]-$F[5]; print "$F[6] $F[13]\n";}' depth_vpvs.txt | psxy -O -K -JX6i -R -Sc.1 -G255/0/0  >> ${OUT}.ps
perl -nae 'if ($F[7] >= 1.85 ) {$a=$F[14]-$F[5]; print "$F[6] $F[13]\n";}' depth_vpvs.txt | psxy -O -K -JX6i -R -Sc.1 -G0/0/255  >> ${OUT}.ps
psxy -O -JX -R >> ${OUT}.ps <<END
0 0.4
100 0.4
END
pstopdf ${OUT}.ps
rm ${OUT}.ps

echo EARS - Crust2 vs vpvs for all
#
OUT=ears-crust2_vs_vpvs
perl -nae '$a=$F[14]-$F[5]; print "$F[7] $a\n"' depth_vpvs.txt | psxy -K -JX6i -R1.6/2.2/-80/80 -Sc.1 -B.10:"Vp/Vs":/10:"Crust2.0 Thickness - EARS Thickness"::."EARS - Crust2.0 vs Vp/Vs":WeSn > ${OUT}.ps
psxy -O -JX -R >> ${OUT}.ps <<END
0 0
100 100
END
pstopdf ${OUT}.ps
rm ${OUT}.ps

#
echo Ears complexity map
#
OUT=world_complexity
psbasemap -K  -X1i -Y1i -JQ0/10i -R-180/180/-90/90  -B20 -U  > ${OUT}.ps
pscoast -K -O -A10000 -JQ -R   -W -Di    >> ${OUT}.ps
cat depth_vpvs.txt | perl -nae '$d = $F[13];print "$F[4] $F[3] $d\n"' - | psxy -K -O -JQ -R -Ccomplexity.cpt -St0.15 >> ${OUT}.ps
psscale -O -K -Y4.2i -D6.5i/2i/7.5c/1.25ch -B.1 -Ccomplexity.cpt  >> ${OUT}.ps

pstext -O -JQ -R >> ${OUT}.ps <<END
0 0 12 0 0 1 .
END
pstopdf ${OUT}.ps
rm ${OUT}.ps

#open ${OUT}.pdf

######################
# crust types:
######################
/bin/rm -f lines.ps
psxy -K -JX6i -R0/80/0/80 -M > lines.ps <<END
0 0
100 100
>>
0 2.5
100 102.5
>>
0 -2.5
100 97.5
>>
0 5
100 105
>>
0 -5
100 95
>>
0 -10
100 90
>>
0 10
100 110
END

echo EARS vs Crust2 for all
#
OUT=ears_crust2_H
cp lines.ps ${OUT}.ps
perl -nae 'print "$F[14] $F[5]\n";' depth_vpvs.txt | psxy -O -JX -R -Sc.1 -B10:"Crust2.0 Thickness":/:"EARS Thickness"::."EARS vs Crust2.0":WeSn >> ${OUT}.ps
pstopdf ${OUT}.ps
rm ${OUT}.ps


OUT=Archean
echo $OUT
cp lines.ps ${OUT}.ps
cat depth_vpvs.txt | perl -nae 'if ($F[19]=~/^[FG]./) {print "$F[14] $F[5]\n";}' - | psxy -O -JX6i -R0/80/0/80 -Sc.1 -B10:"Crust2.0 Thickness":/10:"EARS Thickness"::."EARS vs Crust2.0 for $OUT":WeSn >> ${OUT}.ps
pstopdf ${OUT}.ps
rm ${OUT}.ps
#
echo $OUT histogram 
TITLE=Diff to Crust2 Histogram ${OUT}
OUT=${OUT}_histo
perl -nae 'if ($F[19]=~/^[FG]./) {$a=abs($F[14]-$F[5]);print "$a\n";}' depth_vpvs.txt | pshistogram -JXh -W2.5 -L1g -V -B:."${TITLE}": > ${OUT}.ps
pstopdf ${OUT}.ps 
rm ${OUT}.ps 

OUT=Platform
echo $OUT
cp lines.ps ${OUT}.ps
cat depth_vpvs.txt | perl -nae 'if ($F[19]=~/^[DE]./) {print "$F[14] $F[5]\n";}' - | psxy -O -JX6i -R0/80/0/80 -Sc.1 -B10:"Crust2.0 Thickness":/10:"EARS Thickness"::."EARS vs Crust2.0 for $OUT":WeSn >> ${OUT}.ps
pstopdf ${OUT}.ps
rm ${OUT}.ps
#
echo $OUT histogram 
TITLE=Diff to Crust2 Histogram ${OUT}
OUT=${OUT}_histo
perl -nae 'if ($F[19]=~/^[DE]./) {$a=abs($F[14]-$F[5]);print "$a\n";}' depth_vpvs.txt | pshistogram -JXh -W2.5 -L1g -V -B:."${TITLE}": > ${OUT}.ps
pstopdf ${OUT}.ps 
rm ${OUT}.ps 

OUT=early_mid_proter
echo $OUT
cp lines.ps ${OUT}.ps
cat depth_vpvs.txt | perl -nae 'if ($F[19]=~/^[H]./) {print "$F[14] $F[5]\n";}' - | psxy -O -JX6i -R0/80/0/80 -Sc.1 -B10:"Crust2.0 Thickness":/10:"EARS Thickness"::."EARS vs Crust2.0 for $OUT":WeSn >> ${OUT}.ps
pstopdf ${OUT}.ps
rm ${OUT}.ps
#
echo $OUT histogram 
TITLE=Diff to Crust2 Histogram ${OUT}
OUT=${OUT}_histo
perl -nae 'if ($F[19]=~/^[H]./) {$a=abs($F[14]-$F[5]);print "$a\n";}' depth_vpvs.txt | pshistogram -JXh -W2.5 -L1g -V -B:."${TITLE}": > ${OUT}.ps
pstopdf ${OUT}.ps 
rm ${OUT}.ps 

OUT=late_proter
echo $OUT
cp lines.ps ${OUT}.ps
cat depth_vpvs.txt | perl -nae 'if ($F[19]=~/^[I]./) {print "$F[14] $F[5]\n";}' - | psxy -O -JX6i -R0/80/0/80 -Sc.1 -B10:"Crust2.0 Thickness":/10:"EARS Thickness"::."EARS vs Crust2.0 for $OUT":WeSn >> ${OUT}.ps
pstopdf ${OUT}.ps
rm ${OUT}.ps
#
echo $OUT histogram 
TITLE=Diff to Crust2 Histogram ${OUT}
OUT=${OUT}_histo
perl -nae 'if ($F[19]=~/^[I]./) {$a=abs($F[14]-$F[5]);print "$a\n";}' depth_vpvs.txt | pshistogram -JXh -W2.5 -L1g -V -B:."${TITLE}": > ${OUT}.ps
pstopdf ${OUT}.ps 
rm ${OUT}.ps 

OUT=extended_crust
echo $OUT
cp lines.ps ${OUT}.ps
cat depth_vpvs.txt | perl -nae 'if ($F[19]=~/^[MN]./) {print "$F[14] $F[5]\n";}' - | psxy -O -JX6i -R0/80/0/80 -Sc.1 -B10:"Crust2.0 Thickness":/10:"EARS Thickness"::."EARS vs Crust2.0 for $OUT":WeSn >> ${OUT}.ps
pstopdf ${OUT}.ps
rm ${OUT}.ps
#
echo $OUT histogram 
TITLE=Diff to Crust2 Histogram ${OUT}
OUT=${OUT}_histo
perl -nae 'if ($F[19]=~/^[MN]./) {$a=abs($F[14]-$F[5]);print "$a\n";}' depth_vpvs.txt | pshistogram -JXh -W2.5 -L1g -V -B:."${TITLE}": > ${OUT}.ps
pstopdf ${OUT}.ps 
rm ${OUT}.ps 

OUT=orogen
echo $OUT
cp lines.ps ${OUT}.ps
cat depth_vpvs.txt | perl -nae 'if ($F[19]=~/^[OPQR]./) {print "$F[14] $F[5]\n";}' - | psxy -O -JX6i -R0/80/0/80 -Sc.1 -B10:"Crust2.0 Thickness":/10:"EARS Thickness"::."EARS vs Crust2.0 for $OUT":WeSn >> ${OUT}.ps
pstopdf ${OUT}.ps
rm ${OUT}.ps
#
echo $OUT histogram 
TITLE=Diff to Crust2 Histogram ${OUT}
OUT=${OUT}_histo
perl -nae 'if ($F[19]=~/^[OPQR]./) {$a=abs($F[14]-$F[5]);print "$a\n";}' depth_vpvs.txt | pshistogram -JXh -W2.5 -L1g -V -B:."${TITLE}": > ${OUT}.ps
pstopdf ${OUT}.ps 
rm ${OUT}.ps 

OUT=Phanerozoic
echo $OUT
cp lines.ps ${OUT}.ps
cat depth_vpvs.txt | perl -nae 'if ($F[19]=~/^[Z]./) {print "$F[14] $F[5]\n";}' - | psxy -O -JX6i -R0/80/0/80 -Sc.1 -B10:"Crust2.0 Thickness":/10:"EARS Thickness"::."EARS vs Crust2.0 for $OUT":WeSn >> ${OUT}.ps
pstopdf ${OUT}.ps
rm ${OUT}.ps
#
echo $OUT histogram 
TITLE=Diff to Crust2 Histogram ${OUT}
OUT=${OUT}_histo
perl -nae 'if ($F[19]=~/^[Z]./) {$a=abs($F[14]-$F[5]);print "$a\n";}' depth_vpvs.txt | pshistogram -JXh -W2.5 -L1g -V -B:."${TITLE}": > ${OUT}.ps
pstopdf ${OUT}.ps 
rm ${OUT}.ps 

OUT=margin_shield_trans
echo $OUT
cp lines.ps ${OUT}.ps
cat depth_vpvs.txt | perl -nae 'if ($F[19]=~/^[T]./) {print "$F[14] $F[5]\n";}' - | psxy -O -JX6i -R0/80/0/80 -Sc.1 -B10:"Crust2.0 Thickness":/10:"EARS Thickness"::."EARS vs Crust2.0 for $OUT":WeSn >> ${OUT}.ps
pstopdf ${OUT}.ps
rm ${OUT}.ps
#
echo $OUT histogram 
TITLE=Diff to Crust2 Histogram ${OUT}
OUT=${OUT}_histo
perl -nae 'if ($F[19]=~/^[T]./) {$a=abs($F[14]-$F[5]);print "$a\n";}' depth_vpvs.txt | pshistogram -JXh -W2.5 -L1g -V -B:."${TITLE}": > ${OUT}.ps
pstopdf ${OUT}.ps 
rm ${OUT}.ps 

OUT=margin_shield
echo $OUT
cp lines.ps ${OUT}.ps
cat depth_vpvs.txt | perl -nae 'if ($F[19]=~/^[U]./) {print "$F[14] $F[5]\n";}' - | psxy -O -JX6i -R0/80/0/80 -Sc.1 -B10:"Crust2.0 Thickness":/10:"EARS Thickness"::."EARS vs Crust2.0 for $OUT":WeSn >> ${OUT}.ps
pstopdf ${OUT}.ps
rm ${OUT}.ps
#
echo $OUT histogram 
TITLE=Diff to Crust2 Histogram ${OUT}
OUT=${OUT}_histo
perl -nae 'if ($F[19]=~/^[U]./) {$a=abs($F[14]-$F[5]);print "$a\n";}' depth_vpvs.txt | pshistogram -JXh -W2.5 -L1g -V -B:."${TITLE}": > ${OUT}.ps
pstopdf ${OUT}.ps 
rm ${OUT}.ps 

OUT=rift
echo $OUT
cp lines.ps ${OUT}.ps
cat depth_vpvs.txt | perl -nae 'if ($F[19]=~/^[X]./) {print "$F[14] $F[5]\n";}' - | psxy -O -JX6i -R0/80/0/80 -Sc.1 -B10:"Crust2.0 Thickness":/10:"EARS Thickness"::."EARS vs Crust2.0 for $OUT":WeSn >> ${OUT}.ps
pstopdf ${OUT}.ps
rm ${OUT}.ps
#
echo $OUT histogram 
TITLE=Diff to Crust2 Histogram ${OUT}
OUT=${OUT}_histo
perl -nae 'if ($F[19]=~/^[X]./) {$a=abs($F[14]-$F[5]);print "$a\n";}' depth_vpvs.txt | pshistogram -JXh -W2.5 -L1g -V -B:."${TITLE}": > ${OUT}.ps
pstopdf ${OUT}.ps 
rm ${OUT}.ps 

OUT=other
echo $OUT
cp lines.ps ${OUT}.ps
cat depth_vpvs.txt | perl -nae 'if ($F[19]=~/^[^DEFGHIMNOPQRTUXZ]./) {print "$F[14] $F[5]\n";}' - | psxy -O -JX6i -R0/80/0/80 -Sc.1 -B10:"Crust2.0 Thickness":/10:"EARS Thickness"::."EARS vs Crust2.0 for $OUT":WeSn >> ${OUT}.ps
pstopdf ${OUT}.ps
rm ${OUT}.ps
#
echo $OUT histogram 
TITLE=Diff to Crust2 Histogram ${OUT}
OUT=${OUT}_histo
perl -nae 'if ($F[19]=~/^[^DEFGHIMNOPQRTUXZ]./) {$a=abs($F[14]-$F[5]);print "$a\n";}' depth_vpvs.txt | pshistogram -JXh -W2.5 -L1g -V -B:."${TITLE}": > ${OUT}.ps
pstopdf ${OUT}.ps 
rm ${OUT}.ps 

#


#
echo EARS vs Crust2 for IU/II/IC
#
OUT=GSNears_crust2_H
grep 'I[ICU]' depth_vpvs.txt | perl -nae 'print "$F[14] $F[5]\n"' - | psxy -K -JX6i -R0/80/0/80 -Sc.1 -B10:"Crust2.0 Thickness":/:"EARS Thickness"::."EARS vs Crust2.0 for GSN":WeSn > ${OUT}.ps
psxy -O -JX -R >> ${OUT}.ps <<END
0 0
100 100
>>
0 2.5
100 102.5
>>
0 -2.5
100 97.5
>>
0 5
100 105
>>
0 -5
100 95
END
pstopdf ${OUT}.ps
rm ${OUT}.ps

#
echo  EARS vs Crust2 for IU/II/IC Vp/Vs
#
OUT=GSNears_crust2_vpvs
grep 'I[ICU]' depth_vpvs.txt | perl -nae 'print "$F[10] $F[4]\n"' - | psxy -K -JX6i -R1.5/2.2/1.5/2.2 -Sc.1 -B.1:"Crust2.0 VpVs":/:"EARS VpVs"::."EARS vs Crust2.0 for GSN":WeSn > ${OUT}.ps
psxy -O -JX -R >> ${OUT}.ps <<END
0 0
100 100
>>
0 2.5
100 102.5
>>
0 -2.5
100 97.5
>>
0 5
100 105
>>
0 -5
100 95
END
pstopdf ${OUT}.ps
rm ${OUT}.ps

#
echo  EARS vs Crust2 for XM99
#
grep 'XM' depth_vpvs.txt | perl -nae 'print "$F[9] $F[3]\n"' - | psxy -K -JX6i -R0/80/0/80 -Sc.1 -B10:"Crust2.0 Thickness":/:"EARS Thickness"::."EARS vs Crust2.0":WeSn > XM99ears_crust2_H.ps
psxy -O -JX -R -M >> XM99ears_crust2_H.ps <<END
0 0
100 100
>>
0 2.5
100 102.5
>>
0 -2.5
100 97.5
>>
0 5
100 105
>>
0 -5
100 95
END
pstopdf XM99ears_crust2_H.ps
rm XM99ears_crust2_H.ps

#
echo EARS vs David Wilson for XM99
#
python compareXM99.py | perl -nae 'print "$F[0] $F[1] $F[2]\n"' | psxy -K -H1 -JX6i -R20/70/20/70 -S.2 -G1 -B10:"Wilson Thickness":/:"EARS Thickness"::."EARS vs D. Wilson":WeSn > XM99ears_wilson_H.ps
psxy -O -JX -R -M >> XM99ears_wilson_H.ps <<END
0 0
100 100
>>
0 2.5
100 102.5
>>
0 -2.5
100 97.5
>>
0 5
100 105
>>
0 -5
100 95
END
pstopdf XM99ears_wilson_H.ps
rm XM99ears_wilson_H.ps

#
echo EARS and Wilson vs lon for XM99
#
perl -nae 'print "10$F[2] $F[5]\n"' ../DavidWilson/ristrastationvels.txt | psxy -K -JX6i -R-111/-103/0/200 -Sd.2 -G200/21/21  > XM99ears_wilson_lon_H.ps
grep 'XM99' depth_vpvs.txt | perl -nae 'print "$F[2] $F[3]\n"' - | psxy -O -JX -R -Sc.2 -G200/20/200 -B1:"Longitude":/50:"Thickness"::."EARS vs Wilson":WeSn >> XM99ears_wilson_lon_H.ps
pstopdf XM99ears_wilson_lon_H.ps
rm XM99ears_wilson_lon_H.ps

#
echo Ears H california map
#
psbasemap -K  -X1i -Y1i -JM6i -R-125/-115/32/44 -P  -B2 -U  > california.ps
grdimage crust2_180.grd -Credblue.cpt -JM -R -K -O >> california.ps
pscoast -K -O -A10000 -JM -R   -W -Di -N2 -N1  >> california.ps
### for diff to crust2
#cat depth_vpvs.txt |  perl -nae '$d = $F[3]-$F[9];print "$F[2] $F[1] $d\n"' - | psxy -K -O -JM -R -Cdiffcrust2.cpt -St0.35 >> california.ps
### for thickness
cat depth_vpvs.txt |  perl -nae '$d = $F[3];print "$F[2] $F[1] $d\n"' - | psxy -K -O -JM -R -Credblue.cpt -St0.35 >> california.ps

pstext -O -JM -R >> california.ps <<END
0 0 12 0 0 1 .
END
pstopdf california.ps
rm california.ps
echo done california

#
echo Ears H usa map
#
psbasemap -K  -X1i -Y1i -JM10i -R-125/-65/22/50  -B10 -U  > usa_H.ps
grdimage crust2_180.grd -Ccrust2.cpt -JM -R -K -O >> usa_H.ps
pscoast -K -O -A10000 -JM -R   -W -Di -N2 -N1  >> usa_H.ps
cat depth_vpvs.txt |   perl -nae '$d = $F[3];print "$F[2] $F[1] $d\n"' - | psxy -K -O -JM -R -Ccrust2.cpt -St0.45 >> usa_H.ps
#cat depth_vpvs.txt | grep US |  perl -nae '$d = $F[3]-$F[9];print "$F[2] $F[1] $d\n"' - | psxy -K -O -JM -R -Cdiffcrust2.cpt -St0.35 >> usa_H.ps

pstext -O -JM -R >> usa_H.ps <<END
0 0 12 0 0 1 .
END
pstopdf usa_H.ps
rm usa_H.ps
echo done usa

# scale for world and usa
psscale -D6.5i/2i/7.5c/1.25c -B10 -Ccrust2.cpt   > scale_H.ps
pstopdf scale_H.ps
rm scale_H.ps

#
echo Ears H map GSN
#
psbasemap -K  -X1i -Y1i -JQ0/10i -R-180/180/-90/90  -B20 -U  > gsn_world_H.ps
grdimage crust2_180.grd -Credblue.cpt -JQ -R -K -O >> gsn_world_H.ps
pscoast -K -O -A10000 -JQ -R   -W -Di    >> gsn_world_H.ps
cat depth_vpvs.txt | grep 'I[ICU]' | perl -nae '$d = $F[3];print "$F[2] $F[1] $d\n"' - | psxy -K -O -JQ -R -Credblue.cpt -St0.35 >> gsn_world_H.ps
cat depth_vpvs.txt | grep 'I[ICU]' | perl -nae '$d = $F[3];print "$F[2] $F[1] $d\n"' - | psxy -K -O -JQ -R  -St0.35 >> gsn_world_H.ps
#cat depth_vpvs.txt  | perl -nae '$d = $F[3]-$F[9];print "$F[2] $F[1] $d\n"' - | psxy -K -O -JQ -R -Cdiffcrust2.cpt -St0.35 >> gsn_world_H.ps

pstext -O -JQ -R >> gsn_world_H.ps <<END
0 0 12 0 0 1 .
END
pstopdf gsn_world_H.ps
rm gsn_world_H.ps

#
echo ears vs Lupei for S Cal
#
python compareZhu.py | perl -nae 'print "$F[0] $F[1] $F[2] $F[3]\n"' | psxy -K -H1 -JX6i -R20/50/20/50 -Sc.1 -B10:"Zhu Thickness":/:"EARS Thickness"::."EARS vs L. Zhu":WeSn -G0 -Exy > ears_zhu_H.ps
psxy -O -JX -R >> ears_zhu_H.ps <<END
0 0
100 100
END
pstopdf ears_zhu_H.ps
rm ears_zhu_H.ps

