#!/usr/bin/python

f=open('ristrastationvels.txt', 'r')
lines = f.readlines()
dwilsonH = {}
dwilsonVpVs = {}
for l in lines:
  s = l.split()
  dwilsonH["XM99."+s[0]] = s[5]
  dwilsonVpVs["XM99."+s[0]] = s[6]
f.close()

print "# Wilson H VpVs from Ristra"
dwilsonkeys = dwilsonH.keys()
dwilsonkeys.sort()
for sta in dwilsonkeys:
    #  if abs(float(ears[sta]) - float(dwilson[sta])) > 10:
    print sta+'_H='+dwilsonH[sta]
    print sta+'_VpVs='+dwilsonVpVs[sta]



