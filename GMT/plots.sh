#!/bin/sh

rm -f ears_crust2_H.ps

perl -nae 'print "$F[8] $F[4]\n"' ../stackImages25_80.0/depth_vpvs.txt | pshistogram -JXh -W250 -C -L0.5p -V > ears_crust2_H.ps

