#!/bin/bash


FILENAME=westUS.ps
rm -f $FILENAME
touch $FILENAME
pscoast -JM6i -R-130/-100/30/50 -Di -B5/5 -Na -W >> $FILENAME


#curl -o scal.txt 'http://www.seis.sc.edu/ears_tmp/stationLatLonBox.txt?minLat=30&maxLat=35&minLon=-130&maxLon=-100'
grep -v '\.\.\.' scal.txt | cleantxt.pl | perl -nae 'print "$F[3] $F[4]\n"' | psxy -JX6i/-6i  -R-130/-100/0/70 -Sc.1i -H1 -B5/5 > scal.ps


