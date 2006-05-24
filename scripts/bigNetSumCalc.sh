#!/bin/bash

NETS=" AK AT AZ BK CC CD CH CN CT CZ ER G GE GR GT IM KZ LB LD LI LX MN MS NL NM NR PR PS TA TW UO UW XA XB XC XD XE XF XG XH XI XJ XK XL XM XN XO XR XS XT XU XW XY XZ YA YF YK YS "
BIGNETS="CI IC II IU KN TS US"

WOOZNETS="IU KN TS TA TW UO"
HEFFNETS="IC II CI UW "
RABBITNETS="AK AT AZ BK CC CD CH CN CT CZ ER G GE GR GT IM KZ LB LD LI LX MN MS NL NM NR PR PS XA XB XC XD"
YOYONETS="US XE XF XG XH XI XJ XK XL XM XN XO XR XS XT XU XW XY XZ YA YF YK YS"
NULLNETS=""

for net in $NULLNETS ; do
   echo $net
   nice bin/stackCalc      -p ears_remote.prop     -net $net 
   nice bin/qualityControl -p ears_remote.prop -db -net $net 
   nice bin/sumStackCalc   -p ears_remote.prop     -net $net
done
