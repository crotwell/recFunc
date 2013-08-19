#!/bin/bash

for G in 2.5 1.0 0.7 0.4
do
 curl -m 900 -o moma${G}.csv.new 'http://www.seis.sc.edu/ears/comparePriorResult.txt?name=MOMA&gaussian='${G}
 mv moma${G}.csv.new moma${G}.csv

 curl -m 900 -o moma${G}_nocomp.csv.new 'http://www.seis.sc.edu/ears/stationList.txt?netdbid=48&gaussian='${G}
 mv moma${G}_nocomp.csv.new moma${G}_nocomp.csv

done

