
f=open('../DavidWilson/ristrastationvels.txt', 'r')
lines = f.readlines()
dwilson = {}
for l in lines:
  s = l.split()
  dwilson["XM99."+s[0]] = s[5]
f.close()

f=open('depth_vpvs.txt', 'r')
lines = f.readlines()
ears = {}
for l in lines:
  s = l.split()
  ears[s[0]] = s[3]
f.close()


f=open('XM99.grades', 'r')
lines = f.readlines()
ears_grades = {}
for l in lines:
  s = l.split()
  ears_grades[s[0]] = s[1]
  if (ears_grades[s[0]] == 'B'):
    ears_grades[s[0]] = 'D'
f.close()



print "Wilson EARS Station"
dwilsonkeys = dwilson.keys()
dwilsonkeys.sort()
for sta in dwilsonkeys:
    #  if abs(float(ears[sta]) - float(dwilson[sta])) > 10:
    print dwilson[sta], ears[sta], ears_grades[sta], sta



