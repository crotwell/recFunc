#!/bin/bash

NAME=lupei
REGION=-121/-113.5/10/70

FILENAME=${NAME}H.ps
/bin/rm -f $FILENAME

touch $FILENAME
perl -nae 'print "$F[4] $F[5] $F[6] $F[6]\n"' ${NAME}.txt | psxy -K -P -H2 -JX7i/-4.5i -R${REGION} -B2/2 -Sc.1i -G255/0/0 -Ey >> $FILENAME
perl -nae 'print "$F[4] $F[5] 9 0 1 ML .$F[1]\n"' ${NAME}.txt | pstext  -O -K  -H2 -JX -R >> $FILENAME

perl -nae 'print "$F[4] $F[13]\n"' ${NAME}.txt | psxy -O -H2 -JX -R -Ss.1i -G0/255/0 >> $FILENAME

FILENAME=alphaH.ps
REGION=-1/53.5/10/70
echo  $FILENAME
/bin/rm -f $FILENAME
touch $FILENAME
sort ${NAME}.txt | grep -v 'EARS' | grep -v 'Net' | perl -nae 'print "$. $F[5] $F[6] $F[6]\n"'  | psxy -K -P  -JX7i/-4.5i -R${REGION} -B2/2:"Thickness": -Sc.1i -G155 -Ey >> $FILENAME
sort ${NAME}.txt | grep -v 'EARS' | grep -v 'Net' | perl -nae 'print "$. $F[5] 9 0 1 ML .$F[1]\n"'  | pstext  -O -K  -JX -R >> $FILENAME

sort ${NAME}.txt | grep -v 'EARS' | grep -v 'Net' | perl -nae 'print "$. $F[13]\n"'  | psxy -O -JX -R -Ss.1i -G0/0/0 >> $FILENAME

FILENAME=errorSortH.ps
REGION=0/47/10/70
echo  $FILENAME
/bin/rm -f $FILENAME
touch $FILENAME
perl -nae '$a=abs($F[5]-$F[13]);print "$a @F\n"'  ${NAME}.txt | sort -n | perl -nae '$F[0]=" ";print "@F\n"' | grep -v 'EARS' | grep -v 'Net' > errorSort.txt
cat errorSort.txt | perl -nae 'print "$. $F[5] $F[6] $F[6]\n"'  | psxy -K   -JX7i/-4.5i -R${REGION} -B5/5:"Thickness":Wesn -Sc.1i -G155 -Ey >> $FILENAME

cat errorSort.txt | perl -nae 'print "$. $F[13]\n"'  | psxy -O -K -JX -R -Ss.1i -G0 >> $FILENAME

psxy -O -K -JX -R -Ss.1i -G0 >> $FILENAME <<END
4 60
END
pstext -O -K -JX -R >> $FILENAME <<END
5 60 9 0 1 ML Zhu and Kanamori
END
psxy -O -K -JX -R -Sc.1i -G155 >> $FILENAME <<END
4 63
END
pstext -O -K -JX -R >> $FILENAME <<END
5 63 9 0 1 ML EARS
END
cat errorSort.txt | perl -nae 'print "$. 20 9 90 1 ML $F[0].$F[1]\n"'  | pstext  -O -Y0.9i -JX -R >> $FILENAME
