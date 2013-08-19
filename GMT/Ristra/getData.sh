#!/bin/bash

curl -m 900 -o ristra.csv.new 'http://www.seis.sc.edu/ears/comparePriorResult.txt?name=Wilson&gaussian=2.5'
mv ristra.csv.new ristra.csv
