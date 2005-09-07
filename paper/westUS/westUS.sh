#!/bin/bash

#curl -o scal.txt 'http://www.seis.sc.edu/ears_tmp/stationLatLonBox.html?minLat=30&maxLat=33&minLon=-113&maxLon=-119'

FILENAME=westUS.ps
touch $FILENAME
pscoast -JM6 -R-120/-110/30/45 -Di -B5/5 -Na -W >> $FILENAME


