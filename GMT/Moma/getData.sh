#!/bin/bash

curl -m 900 -o moma.csv.new 'http://www.seis.sc.edu/ears/comparePriorResult.txt?name=MOMA&gaussian=2.5'
mv moma.csv.new moma.csv

curl -m 900 -o moma_nocomp.csv.new 'http://www.seis.sc.edu/ears/stationList.txt?netdbid=48&gaussian=2.5'
mv moma_nocomp.csv.new moma_nocomp.csv


