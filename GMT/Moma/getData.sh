#!/bin/bash

curl -m 900 -o moma.csv.new 'http://www.seis.sc.edu/ears/comparePriorResult.txt?name=MOMA&gaussian=2.5'
mv moma.csv.new moma.csv
