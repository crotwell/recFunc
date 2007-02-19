#! /usr/bin/python

import csv, os, time, sys
from distaz import DistAz
sys.path.append('../surface')
import ears

def blockMean(dataFile, outFilename, region='-126/-114/30/50', minEQ=0, blockSize='0.5', columns=[3,2,5], extras='', maxComplexity=1):
    ears.mapCleanUp(outFilename)
    out = ' >> '+outFilename
    gmt=os.popen('blockmean -R'+region+' -I'+blockSize+' '+extras+' '+out, 'w')
    ears.readData(dataFile, gmt, region=region, minEQ=minEQ, columns=columns, maxComplexity=maxComplexity)
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
    ears.mapCleanUp(triangleFile)
    os.popen('triangulate '+blockDataFile+' > '+triangleFile, 'w').close()
    triangleSegments = dataFile+'_triangleSeg'
    ears.mapCleanUp(triangleSegments)
    os.popen('triangulate -M '+blockDataFile+' > '+triangleSegments, 'w').close()
    os.popen('pscontour '+blockDataFile+' -K -R'+region+' -J'+proj+' -B2f1WSNe -I -Cno_green_25_50.cpt  '+extras+' '+out, 'w').close()
#    os.popen('psxy -O -K -R'+region+' -J'+proj+' -B2f1WSNe -M '+triangleSegments+' -W0.5p  '+extras+' '+out, 'w').close()
    os.popen('pscoast -O -R'+region+' -J'+proj+'  -Di -Na -W4  '+extras+' '+out, 'w').close()
    ears.ps2pdf(outFilename)

def makeGridMap(dataFile, outFilename, proj='M5.0i', region='-126/-114/30/50', minEQ=0, extras='', blockSize='0.5', contour='5', columns=[3,2,5], cpt='no_green_25_50.cpt', mask=0.75, maxComplexity=1):
    ears.psstart(outFilename, extras=extras)
    annotate=2*float(contour)
    out = ' >> '+outFilename
    blockDataFile = dataFile+"_block"
    surfaceGrid = dataFile+"_grid"
    ears.mapCleanUp(blockDataFile)
    blockMean(dataFile, blockDataFile, region=region, minEQ=minEQ, blockSize=blockSize, columns=columns,  maxComplexity=maxComplexity)
    os.popen('surface '+blockDataFile+' -R'+region+' -T.35 -I'+blockSize+' -G'+surfaceGrid, 'w').close()
    gmt=os.popen('psmask  -I%s -J%s -R%s -S%s -O -K %s %s' % (blockSize, proj, region, mask, extras, out), 'w')
    ears.readData(dataFile, gmt, region=region, minEQ=minEQ, maxComplexity=maxComplexity)
    gmt.close()
    os.popen('grdimage %s -J%s -C%s  -S4 -K -O %s %s' % (surfaceGrid, proj, cpt, extras, out), 'w').close()
    os.popen('psmask -C -O -K %s' % (out), 'w').close()
#    os.popen('grdcontour %s -J%s -B4f2WSne -C%s -A%s -G3i/10 -S4 -K -O %s %s' % (surfaceGrid, proj, contour, annotate, extras, out), 'w').close()
    coastLabelsScale(outFilename, proj=proj, region=region, extras=extras, cpt=cpt)

    ears.psfinish(outFilename)
    ears.ps2pdf(outFilename)

def makeXYMap(dataFile, outFilename, proj='M5.0i', region='-126/-114/30/50', minEQ=0, extras='', columns=[3,2,5], cpt='no_green_25_50.cpt', maxComplexity=1, labelSta=False):
    out = ' >> '+outFilename  
    baseGmtArgs = '-J'+proj+' -R'+region+' -K '+extras
    os.popen('gmtset BASEMAP_TYPE plain', 'w').close()
    ears.psstart(outFilename, extras=extras)
    baseGmtArgs = baseGmtArgs + ' -O '
    os.popen('psbasemap '+baseGmtArgs+' -B2  '+out, 'w').close()
    coastLabelsScale(outFilename, proj=proj, region=region, extras=extras, cpt=cpt)
    gmt= os.popen('psxy '+baseGmtArgs+' -St.2i -C'+cpt+' -W'+out, 'w')
    ears.readData(dataFile, gmt, region=region, minEQ=minEQ, maxComplexity=maxComplexity)
    gmt.close()
    if labelSta:
       gmt= os.popen('pstext '+baseGmtArgs+'  -W'+out, 'w')
       results = csv.reader(open(dataFile, 'r'))
       for row in results: 
           if row[0].startswith('#'):
               continue
           if onlyOneNet != '' and onlyOneNet != row[0]:
               continue
	   if int(row[12]) < minEQ or float(row[13]) > maxComplexity:
	       continue
           thick = row[5].replace(' km','')
           gmt.write(row[3]+"  "+ row[2]+" 8 0 4 BL ."+row[0]+'.'+row[1]+"\n") 
       gmt.close()

    ears.psfinish(outFilename)
    ears.ps2pdf(outFilename)

def coastLabelsScale(outFilename, proj='M5.0i', region='-126/-114/30/50', extras='', cpt='no_green_25_50.cpt'):
    out = ' >> '+outFilename
    os.popen('pscoast -R'+region+' -B2f2WSne -J -O -K -Sgray -W0.25p '+extras+out, 'w').close() 
    os.popen('pscoast -O -K -R'+region+' -J'+proj+'  -Di -Na -W4  '+extras+' '+out, 'w').close()

    showCMB=False
    if showCMB:
	gmt=os.popen('psxy -O -K -R'+region+' -J'+proj+' -W2 -St0.2i '+extras+' '+out, 'w')
	gmt.write('-118.17 37.5\n-120.39 38.03\n')
	gmt.close()
	gmt=os.popen('pstext -O -K -R'+region+' -J'+proj+'  '+extras+' '+out, 'w')
	gmt.write('-118.0 37.5 12 0 4 LM TA.S08C\n-120.22 38.03 12 0 4 LM BK.CMB\n')
	gmt.close()
    os.popen("gmtset  LABEL_FONT_SIZE 12").close()
    os.popen('psscale -B5/:"(km)": -D.25i/1.0i/1.5i/.2i -C%s -O -K %s' % (cpt, out), 'w').close()

ears.gmtUseNegDegree()
minEQ=5

#trangleMap('allWestCoast.csv', 'allWestCoast.ps', extras='-P')
#trangleMap('allUS.csv', 'allUS.ps', region='-126/-65/23/50', proj='M10i')

#maskLargeTriangles('allUS.csv_block', 'allUS.csv_triangles', 'allUS.xy_mask')
#makeGridMap('allUS.csv', 'allUSGrid.ps', region='-126/-65/23/50', proj='M10i')


makeGridMap('allWestCoast.csv', 'allWestCoastGrid.ps', proj='M4.0i', region='-126/-115/32/50', extras='-P', blockSize='0.25', contour='5', columns=[3,2,5], minEQ=minEQ, mask=0.75,  maxComplexity=.75)
makeXYMap('allWestCoast.csv', 'allWestCoastXY.ps', proj='M4.0i', region='-126/-115/32/50', extras='-P', columns=[3,2,5], minEQ=minEQ,  maxComplexity=.75)

# different masks
proj='M3.0i'
#makeGridMap('allWestCoast.csv', 'mask0.25.ps', proj=proj, region='-126/-115/32/50', extras='-P', blockSize='0.25', contour='2.5', columns=[3,2,5], minEQ=minEQ, mask=0.25)
#makeGridMap('allWestCoast.csv', 'mask0.5.ps', proj=proj, region='-126/-115/32/50', extras='-P', blockSize='0.25', contour='2.5', columns=[3,2,5], minEQ=minEQ, mask=0.5)
#makeGridMap('allWestCoast.csv', 'mask0.75.ps', proj=proj, region='-126/-115/32/50', extras='-P', blockSize='0.25', contour='2.5', columns=[3,2,5], minEQ=minEQ, mask=0.75)
#makeGridMap('allWestCoast.csv', 'mask1.0.ps', proj=proj, region='-126/-115/32/50', extras='-P', blockSize='0.25', contour='2.5', columns=[3,2,5], minEQ=minEQ, mask=1.0)
#makeGridMap('allWestCoast.csv', 'mask2.0.ps', proj=proj, region='-126/-115/32/50', extras='-P', blockSize='0.25', contour='2.5', columns=[3,2,5], minEQ=minEQ, mask=2.0)

# different maxComplexity
mask=.75
#makeGridMap('allWestCoast.csv', 'allWestCoastGrid_c25.ps', proj='M4.0i', region='-126/-115/32/50', extras='-P', blockSize='0.25', contour='2.5', columns=[3,2,5], minEQ=minEQ, mask=mask,  maxComplexity=.25)
#makeGridMap('allWestCoast.csv', 'allWestCoastGrid_c50.ps', proj='M4.0i', region='-126/-115/32/50', extras='-P', blockSize='0.25', contour='2.5', columns=[3,2,5], minEQ=minEQ, mask=mask,  maxComplexity=.5)
#makeGridMap('allWestCoast.csv', 'allWestCoastGrid_c75.ps', proj='M4.0i', region='-126/-115/32/50', extras='-P', blockSize='0.25', contour='2.5', columns=[3,2,5], minEQ=minEQ, mask=mask,  maxComplexity=.75)
#makeGridMap('allWestCoast.csv', 'allWestCoastGrid_c100.ps', proj='M4.0i', region='-126/-115/32/50', extras='-P', blockSize='0.25', contour='2.5', columns=[3,2,5], minEQ=minEQ, mask=mask,  maxComplexity=1.0)

#makeGridMap('allWestCoast.csv', 'allWestCoastVpVs.ps', proj=proj, region='-126/-115/32/50', extras='-P', blockSize='0.25', contour='.1', columns=[3,2,7], minEQ=minEQ, cpt='no_green_vpvs.cpt')

#makeGridMap('allUS.csv', 'allUSGrid.ps', proj='M7.5i', region='-126/-66/25/50', extras='', blockSize='0.25', contour='2.5', columns=[3,2,5], minEQ=minEQ)
