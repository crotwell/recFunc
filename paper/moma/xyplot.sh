#!/bin/bash

gmtset PLOT_DEGREE_FORMAT -D ANNOT_FONT_PRIMARY Times-Roman
gmtset LABEL_FONT Times-Roman LABEL_FONT_SIZE 12


NAME=moma
REGION=-92/-70/20/70
FILENAME=${NAME}H.ps

/bin/rm -f ${NAME}H.ps

touch ${NAME}H.ps

psxy -K -JX6i/-4i -R${REGION} -Ss.1i -G0 >> $FILENAME <<END
-78 58
END
pstext -O -K -JX -R >> $FILENAME <<END
-77.3 58 9 0 1 ML Li et. al. Vp=6.5 Vp/Vs=1.73
END
psxy -O -K -JX -R -Sa.1i -G0 >> $FILENAME <<END
-78 60
END
pstext -O -K -JX -R >> $FILENAME <<END
-77.3 60 9 0 1 ML Li et. al. Vp=6.6 Vp/Vs=1.80
END
psxy -O -K -JX -R -Sd.1i -G0 >> $FILENAME <<END
-78 62
END
pstext -O -K -JX -R >> $FILENAME <<END
-77.3 62 9 0 1 ML Li et. al. Vp=6.6 Vp/Vs=1.84
END

psxy -O -K -JX -R -Sc.1i -G155 >> $FILENAME <<END
-78 64
END
pstext -O -K -JX -R >> $FILENAME <<END
-77.3 64 9 0 1 ML EARS
END

perl -nae 'print "$F[4] $F[5] $F[6] $F[6]\n"' ${NAME}.txt | psxy -O -K -H2 -JX -R -B2:"Longitude":/5:"Thickness (km)":WSen -Sc.1i -G155 -Ey >> ${FILENAME}

cat ${NAME}.txt | grep 1.73 | perl -nae 'print "$F[4] $F[13]\n"'  | psxy -O -K -JX -R -Ss.1i -G0 >> ${FILENAME}
cat ${NAME}.txt | grep 1.80 | perl -nae 'print "$F[4] $F[13]\n"'  | psxy -O -K -JX -R -Sa.1i -G0 >> ${FILENAME}
cat ${NAME}.txt | grep 1.84 | perl -nae 'print "$F[4] $F[13]\n"'  | psxy -O -K -JX -R -Sd.1i -G0 >> ${FILENAME}

perl -nae '$x=$F[4]+.2;print "$x 30 9 90 1 MC $F[1]\n"' ${NAME}.txt | pstext  -O  -H2 -JX -R -Y1.1i >> ${FILENAME}
