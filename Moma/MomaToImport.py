#!/usr/bin/python

f=open('Fischer.txt', 'r')
l = f.readline()
l = f.readline()
s = l.split()
vp = s[3:6]

l = f.readline()
s = l.split()
vpvs = s[1:4]

lines = f.readlines()

for l in lines:
  s = l.split()
  h = s[6:9]
  for x in [0, 1, 2]:
      if (h[x] != '-'):
          net = 'XA95'
          if (s[0] == 'CCM'): net = 'IU'
          if (s[0] == 'HRV'): net = 'IU'
          print net+' '+s[0]+' '+h[x]+' '+vpvs[x]+' '+vp[x]
f.close()

