#!/usr/bin/python

f=open('ristrastationvels.txt', 'r')
lines = f.readlines()
dwilsonH = {}
dwilsonVpVs = {}
for l in lines:
  s = l.split()
  print 'XM99'+' '+s[0]+' '+s[5]+' '+s[6]+' '+s[4]
f.close()

