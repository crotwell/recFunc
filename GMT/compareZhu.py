
f=open('../LupeiZhu/moho_clean.tbl', 'r')
lines = f.readlines()
lupei = {}
for l in lines:
  s = l.split()
  lupei[s[0]] = s
f.close()

f=open('depth_vpvs.txt', 'r')
lines = f.readlines()
ears = {}
for l in lines:
  s = l.split()
  ears[s[0]] = s
f.close()

print "Zhu EARS Station"
lupeikeys = lupei.keys()
lupeikeys.sort()
for sta in lupeikeys:
    #  if abs(float(ears[sta]) - float(lupei[sta])) > 10:
    if ears.has_key(sta):
      print lupei[sta][6], ears[sta][3], lupei[sta][7], ears[sta][7], sta



