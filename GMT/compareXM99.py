
f=open('../DavidWilson/ristrastationvels.txt', 'r')
lines = f.readlines()
dwilson = {}
for l in lines:
  s = l.split()
  dwilson["XM."+s[0]] = s[5]
f.close()

f=open('depth_vpvs.txt', 'r')
lines = f.readlines()
ears = {}
for l in lines:
  s = l.split()
  ears[s[0]] = s[3]
f.close()

print "EARS Wilson Station"
for sta in dwilson.keys():
  print ears[sta], dwilson[sta], sta



