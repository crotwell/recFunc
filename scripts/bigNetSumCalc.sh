#!/bin/bash

NETS=" AK AT AZ BK CC CD CH CN CT CZ ER G GE GR GT IM KZ LB LD LI LX MN MS NL NM NR PR PS TA TW UO UW XA XB XC XD XE XF XG XH XI XJ XK XL XM XN XO XR XS XT XU XW XY XZ YA YF YK YS "
BIGNETS="CI IC II IU KN TS US"
WOOZNETS="IU KN TS TA TW UO"
HEFFNETS="IC II CI UW XA XB XC XD"
RABBITNETS="AK AT AZ BK CC CD CH CN CT CZ ER G GE GR GT IM KZ LB LD LI LX MN MS NL NM NR PR PS "
YOYONETS="US XE XF XG XH XI XJ XK XL XM XN XO XR XS XT XU XW XY XZ YA YF YK YS"

for net in $YOYONETS ; do
   echo $net
   nice bin/stackCalc  -props ears_remote.prop -net $net 
   nice bin/sumStackCalc -props ears_remote.prop -net $net
done
