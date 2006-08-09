#! /usr/bin/python

import csv, os, time, sys
from distaz import DistAz

def ps2pdf(outFilename):
    ps2pdf = os.popen('pstopdf '+outFilename)
    ps2pdf.close()
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

def readData(dataFile, gmt, columns=[3,2,5], infinityVal=-1,
	     minLat=-90.0, maxLat=90.0, minLon=-180.0, maxLon=180.0, 
	     minEQ=0, sep=' ',  lineEnd='', region=None, 
	     excludeFile='stationsToIgnore.txt'):
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
        if (int(row[12]) < minEQ):
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

def blockMean(dataFile, outFilename, region='-126/-114/30/50', minEQ=0, blockSize='0.5', columns=[3,2,5], extras=''):
    mapCleanUp(outFilename)
    out = ' >> '+outFilename
    gmt=os.popen('blockmean -R'+region+' -I'+blockSize+' '+extras+' '+out, 'w')
    readData(dataFile, gmt, region=region, minEQ=minEQ, columns=columns)
    gmt.close()

def maskLargeTriangles(dataFile, triangleFile, outFilename):
    points = {}
    results=open(dataFile, 'r')
    lineNum = 0
    for line in results:
        row=line.split()
	for i in range(len(row)):
	    row[i] = float(row[i])
	points[lineNum]=row
	lineNum+=1
    triangles = []
    results=open(triangleFile, 'r')
    for line in results:
        row = line.split()
	for i in range(len(row)):
	    row[i] = int(row[i])
	triangles.append(row)
    maxLength = 3
    numLarge=0
    for tri in triangles:
	pointA=points[tri[0]]
	pointB=points[tri[1]]
	pointC=points[tri[2]]
	distAB = DistAz(float(pointA[1]), pointA[0], pointB[1], pointB[0]).getDelta()
	distBC = DistAz(float(pointC[1]), pointC[0], pointB[1], pointB[0]).getDelta()
	distAC = DistAz(float(pointA[1]), pointA[0], pointC[1], pointC[0]).getDelta()
	if distAB > maxLength or distBC > maxLength or distAC > maxLength:
	    print distAB, distBC, distAC
	    numLarge+=1
    print '%d triangles with a side > %f out of %d' % (numLarge, maxLength, len(triangles))
    

def trangleMap(dataFile, outFilename, proj='M5.0i', region='-126/-114/30/50', minEQ=0, extras=''):
    blockDataFile = dataFile+"_block"
    mapCleanUp(blockDataFile)
    blockMean(dataFile, blockDataFile, region=region, minEQ=minEQ)
    mapCleanUp(outFilename)
    out = ' >> '+outFilename
    baseGmtArgs = '-J'+proj+' -R'+region+' -K '
    triangleFile = dataFile+'_triangles'
    mapCleanUp(triangleFile)
    gmt= os.popen('triangulate '+blockDataFile+' > '+triangleFile, 'w')
    gmt.close()
    triangleSegments = dataFile+'_triangleSeg'
    mapCleanUp(triangleSegments)
    gmt= os.popen('triangulate -M '+blockDataFile+' > '+triangleSegments, 'w')
    gmt.close()
    gmt=os.popen('pscontour '+blockDataFile+' -K -R'+region+' -J'+proj+' -B2f1WSNe -I -Cno_green_25_50.cpt  '+extras+' '+out, 'w')
    gmt.close()
#    gmt=os.popen('psxy -O -K -R'+region+' -J'+proj+' -B2f1WSNe -M '+triangleSegments+' -W0.5p  '+extras+' '+out, 'w')
#    gmt.close()
    gmt=os.popen('pscoast -O -R'+region+' -J'+proj+'  -Di -Na -W4  '+extras+' '+out, 'w')
    gmt.close()
    ps2pdf(outFilename)

def makeGridMap(dataFile, outFilename, proj='M5.0i', region='-126/-114/30/50', minEQ=0, extras='', blockSize='0.5', contour='5', columns=[3,2,5], cpt='no_green_25_50.cpt'):
    annotate=2*float(contour)
    mapCleanUp(outFilename)
    out = ' >> '+outFilename
    blockDataFile = dataFile+"_block"
    surfaceGrid = dataFile+"_grid"
    mapCleanUp(blockDataFile)
    blockMean(dataFile, blockDataFile, region=region, minEQ=minEQ, blockSize=blockSize, columns=columns)
    mapCleanUp(outFilename)
    gmt=os.popen('surface '+blockDataFile+' -R'+region+' -T.35 -I'+blockSize+' -G'+surfaceGrid, 'w')
    gmt.close()
    gmt=os.popen('psmask  -I%s -J%s -R%s -S0.75 -K %s %s' % (blockSize, proj, region, extras, out), 'w')
    readData(dataFile, gmt, region=region, minEQ=minEQ)
    gmt.close()
    gmt=os.popen('grdimage %s -J%s -C%s  -S4 -K -O %s %s' % (surfaceGrid, proj, cpt, extras, out), 'w')
    gmt.close()
#    gmt=os.popen('grdcontour %s -J%s -B4f2WSne -C%s -A%s -G3i/10 -S4 -K -O %s %s' % (surfaceGrid, proj, contour, annotate, extras, out), 'w')
#    gmt.close()
    gmt=os.popen('psmask -C -O -K %s' % (out), 'w')
    gmt.close()
    gmt.close()
    gmt=os.popen('pscoast -R'+region+' -B4f2WSne -J -O -K -Sgray -W0.25p '+extras+out, 'w')
    gmt.close() 
    gmt=os.popen('pscoast -O -K -R'+region+' -J'+proj+'  -Di -Na -W4  '+extras+' '+out, 'w')
    gmt.close()
    gmt=os.popen('psscale -B5 -D5i/7i/3i/.3i -C%s -O %s' % (cpt, out), 'w')
    gmt.close()
    ps2pdf(outFilename)


gmtset = os.popen('gmtset OUTPUT_DEGREE_FORMAT=-ddd PLOT_DEGREE_FORMAT=-ddd')
gmtset.close()

#trangleMap('allWestCoast.csv', 'allWestCoast.ps', extras='-P')
#trangleMap('allUS.csv', 'allUS.ps', region='-126/-65/23/50', proj='M10i')

#maskLargeTriangles('allUS.csv_block', 'allUS.csv_triangles', 'allUS.xy_mask')
#makeGridMap('allUS.csv', 'allUSGrid.ps', region='-126/-65/23/50', proj='M10i')
makeGridMap('allWestCoast.csv', 'allWestCoastGrid.ps', proj='M4.75i', region='-126/-115/32/50', extras='-P', blockSize='0.25', contour='2.5', columns=[3,2,5], minEQ=5)

makeGridMap('allWestCoast.csv', 'allWestCoastVpVs.ps', proj='M4.75i', region='-126/-115/32/50', extras='-P', blockSize='0.25', contour='.1', columns=[3,2,7], minEQ=5, cpt='no_green_vpvs.cpt')
