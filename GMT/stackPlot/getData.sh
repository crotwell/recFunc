#!/bin/sh

curl -o data.xyz 'http://www.seis.sc.edu/ears/sumHKStackAsXYZ.txt?netcode=TA&stacode=M06C&minPercentMatch=80&smallestH=25.0&gaussian=2.5'
