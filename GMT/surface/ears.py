
def readData(dataFile, gmt, columns=[3,2,5], infinityVal=-1,
	     minLat=-90.0, maxLat=90.0, minLon=-180.0, maxLon=180.0, 
	     minEQ=0, sep=' ',  lineEnd='', region=None, 
	     excludeFile='stationsToIgnore.txt', maxComplexity=1.0):
    if region is not None:
        latlon = region.split('/')
        minLat=float(latlon[2])
        maxLat=float(latlon[3])
        minLon=float(latlon[0])
        maxLon=float(latlon[1])
    print 'reading %s minLat=%s, maxLat=%s, minLon=%s, maxLon=%s, minEQ=%s' % (dataFile, minLat, maxLat, minLon, maxLon, minEQ)
    excludes=[]
    for line in open(excludeFile, 'r'):
	row = line.split()
	if (len(row) >1):
	    excludes.append(row[0]+' '+row[1])
    results = csv.reader(open(dataFile, 'r'))
    mean=[0,0,0,0,0,0,0,0,0,0,0,0,0]
    num=[0,0,0,0,0,0,0,0,0,0,0,0,0]
    for row in results:
        if row[0].startswith('#'):
            continue
        lat = float(row[2])
        lon = float(row[3])
        if (lat < minLat or lat > maxLat or lon < minLon or lon > maxLon):
            continue
        if int(row[12]) < minEQ and float(row[13]) > maxComplexity:
            continue
	if (row[0]+' '+row[1] in excludes):
	    continue
        line = ''
        for col in columns:
            if row[col].endswith(' km/s'):
		row[col] = row[col].replace(' km/s','')
            if row[col].endswith(' km'):
		row[col] = row[col].replace(' km','')
            if row[col] == 'Infinity' or row[col] == '?':
                row[col] = infinityVal 
            else:
        #       mean[col] = mean[col] + float(row[col])
                num[col] = num[col]+1
            gmt.write(row[col]+sep)
            line = line + row[col]+' '
        gmt.write(lineEnd+'\n')
#       print(line)
    for col in columns:
        if num[col] != 0:
           # mean[col] = mean[col]/num[col]
            #print '%s mean[%i]=%f num=%f' % (dataFile, col, mean[col], num[col])
            pass
