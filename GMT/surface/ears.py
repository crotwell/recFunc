import os, csv

def gmtUseNegDegree():
    gmtset = os.popen('gmtset OUTPUT_DEGREE_FORMAT -ddd PLOT_DEGREE_FORMAT -ddd')
    gmtset.close()

def ps2pdf(outFilename):
    epsFilename=outFilename.replace('.ps','.eps')
    run('eps2eps %s %s'%(outFilename, epsFilename)) 
    run('epstopdf '+epsFilename)
    try:
        #os.remove(outFilename)
        pass
    except OSError:
        pass

def mapCleanUp(outFilename):
    try:
	os.remove(outFilename)
    except OSError:
	pass
    try:
	os.remove(outFilename.replace('.ps', '.pdf'))
    except OSError:
	pass

def psfinish(outFilename, extras=''):
    gmt= os.popen('psxy -JM1 -R0/1/0/1 -St.003i -G255 -O '+extras+' >> '+outFilename, 'w')
    gmt.close()


def psstart(outFilename, extras=''):
    mapCleanUp(outFilename)
    gmt= os.popen('psxy -JM1 -R0/1/0/1 -St.003i -G255 -K '+extras+' > '+outFilename, 'w')
    gmt.close()

def psShift(outFilename, xshift, yshift):
    gmt=os.popen(('psxy -X%f -Y%f -JX -R -O -K >> %s')%(xshift, yshift, outFilename), 'w').close()

def run(cmd):
    os.popen(cmd, 'w').close()

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
    if os.path.exists(excludeFile):
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
	if row[12] == '':
	    continue
        if (lat < minLat or lat > maxLat or lon < minLon or lon > maxLon):
            continue
        if int(row[12]) < minEQ or float(row[13]) > maxComplexity:
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
                num[col] = num[col]+1
            gmt.write(row[col]+sep)
            line = line + row[col]+' '
        gmt.write(lineEnd+'\n')
