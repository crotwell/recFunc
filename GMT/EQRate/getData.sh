#!/bin/bash

#WESTUSNETS="AZ "
WESTUSNETS="AZ BK CC CI CT G II IM IU LB LI NR TA TS UO US UW XC94 XF94 XH94 XJ00 XJ97 XL94 XN00 YA98 YS01"

for net in  $WESTUSNETS ; do
    curl -o data/${net}results.csv 'http://www.seis.sc.edu/ears_dev/eqrate?netcode='${net}'&gaussian=2.5&filetype=text/csv'
done
