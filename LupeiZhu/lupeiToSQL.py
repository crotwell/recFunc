#!/usr/bin/python

f=open('moho_clean.tbl', 'r')
lines = f.readlines()
for l in lines:
  s = l.split()
  if s[9] == '-':
    s[9] = '1.78'
  print s[0]+' '+s[1]+' '+s[7]+' '+s[9]+' 6.3'
f.close()

