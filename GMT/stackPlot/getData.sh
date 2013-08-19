#!/bin/sh

curl -o data.xyz 'http://www.seis.sc.edu/ears/sumHKStackAsXYZ.txt?netcode=TA&stacode=M06C&minPercentMatch=80&smallestH=25.0&gaussian=2.5'

curl -o synth.xyz 'http://www.seis.sc.edu/ears_dev/synthHKAsXYZ.txt?netcode=TA&stacode=M06C&minPercentMatch=80&smallestH=25.0&gaussian=2.5'

curl -o residual.xyz 'http://www.seis.sc.edu/ears_dev/complexResidualAsXYZ.txt?netcode=TA&stacode=M06C&minPercentMatch=80&smallestH=25.0&gaussian=2.5'
