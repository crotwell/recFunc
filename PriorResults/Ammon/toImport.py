#!/usr/bin/python

f=open('VpVs.csv', 'r')
l = f.readline()
lines = f.readlines()

for l in lines:
    s = l.strip().split(',')
    net = 'IU'
    print net+' '+s[0]+' '+s[13]+' '+s[11]+' 6.0'
    print net+' '+s[0]+' '+s[16]+' '+s[14]+' 6.25'
    print net+' '+s[0]+' '+s[19]+' '+s[17]+' 6.5'
    print net+' '+s[0]+' '+s[22]+' '+s[20]+' 6.75'
f.close()

