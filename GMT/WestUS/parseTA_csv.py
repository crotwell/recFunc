#! /usr/bin/python

import csv, os, time, sys

sys.path.append('../surface')
import ears

def etopo(outFilename, proj='M5.5i', region='-126/-114/30/50', shift='', extra='', cptFile='/seis/local/External/GMT/share/cpt/GMT_topo.cpt'):
    out = ' >> '+outFilename
    baseGmtArgs = '-J'+proj+' -R'+region+' -P -K '+extra
    gmt = os.popen('grdimage /data/GMT/Grid/etopo5.grd -T -C'+cptFile+' ' +baseGmtArgs+ out, 'w')
    gmt.close()

def gtopo(outFilename, proj='M5.5i', region='-126/-114/30/50', shift='', extra='', cptFile='/seis/local/External/GMT/share/cpt/GMT_topo.cpt'):
    out = ' >> '+outFilename
    baseGmtArgs = '-J'+proj+' -R'+region+' -P -K '+extra
    gmt = os.popen('grdimage /data/GMT/Grid/gtopo30/w140n40.grd -T -C'+cptFile+' ' +baseGmtArgs+ out, 'w')
    gmt.close()
    gmt = os.popen('grdimage /data/GMT/Grid/gtopo30/w140n90.grd -T -C'+cptFile+' ' +baseGmtArgs+ out, 'w')
    gmt.close()

def crust2(outFilename, proj='M5.5i', region='-126/-114/30/50', shift='', extra='', backgroundGRD='/data/GMT/Grid/Crust2/thickness_180.grd', cptFile='no_green_25_50.cpt'):
    out = ' >> '+outFilename
    baseGmtArgs = '-J'+proj+' -R'+region+' -P -K '+extra
    gmt = os.popen('grdimage '+backgroundGRD+' -T -C'+cptFile+' ' +baseGmtArgs+ out, 'w')
    gmt.close()

    

def makeBigMap(dataFile, outFilename, proj='M5.5i', region='-126/-114/30/50', shift='', extra='', minEQ=0, backgroundGRD='/data/GMT/Grid/Crust2/thickness_180.grd', cptFile='no_green_25_50.cpt', maxComplexity=1, labelSta=False):
    ears.mapCleanUp(outFilename)
    makeMap(dataFile, outFilename, proj, region, shift, extra, minEQ=minEQ, backgroundGRD=backgroundGRD, cptFile=cptFile, maxComplexity=maxComplexity, labelSta=labelSta)
    ears.ps2pdf(outFilename)
    print outFilename

def makeMap(dataFile, outFilename, proj='M5.5i', region='-126/-114/30/50', shift='', extra='', vpvs=False, minEQ=0, backgroundGRD='/data/GMT/Grid/Crust2/thickness_180.grd', cptFile='no_green_25_50.cpt', onlyOneNet='', labelSta=False, maxComplexity=1):
    out = ' >> '+outFilename
    gmt = os.popen('gmtset BASEMAP_TYPE plain')
    gmt.close()
    baseGmtArgs = '-J'+proj+' -R'+region+' -P -K '+extra
    print 'baseGmtArgs='+baseGmtArgs
    gmt= os.popen('psbasemap ' +baseGmtArgs+' -B2 '+shift+'  '+out, 'w')
    gmt.close()
    if baseGmtArgs.find('-O') == -1:
	print 'adding -O'
	baseGmtArgs = baseGmtArgs + ' -O '

    #crust2(outFilename, proj, region, shift, extra+' -O ')
    #etopo(outFilename, proj, region, shift, extra+' -O ')

    gmt= os.popen('pscoast ' +baseGmtArgs+' -Dh -Na -W '+out, 'w')
    gmt.close()

    gmt= os.popen('psxy '+baseGmtArgs+' -St.2i -C'+cptFile+' -W'+out, 'w')
    ears.readData(dataFile, gmt, region=region, minEQ=minEQ, columns=[3,2,5], maxComplexity=maxComplexity, excludeFile='../surface/stationsToIgnore.txt')
    gmt.close()
    
    gmt=os.popen('psscale -B5/:"(km)": -D.25i/1.0i/1.5i/.2i -C%s -O -K %s' % (cptFile, out), 'w')
    gmt.close()
#label
    if labelSta:
       gmt= os.popen('pstext '+baseGmtArgs+'  -W'+out, 'w')
       results = csv.reader(open(dataFile, 'r'))
       for row in results: 
           if row[0].startswith('#'):
               continue
           if onlyOneNet != '' and onlyOneNet != row[0]:
               continue
           thick = row[5].replace(' km','')
           gmt.write(row[3]+"  "+ row[2]+" 8 0 4 BL ."+row[0]+'.'+row[1]+"\n") 
       gmt.close()

    ears.psfinish(outFilename)


def makeLonVsThickBoxes(dataFile, outfilePrefix, minLat, maxLat, incLat, minEQ=0, backgroundGRD='/data/GMT/Grid/Crust2/thickness_180.grd', cptFile='no_green_25_50.cpt', mapLabelSta=False, onlyOneNet=''):
    xyregion='-124/-114/20/60'
    for lat in range(minLat, maxLat, incLat):
	outFilename = outfilePrefix+str(lat)+'.ps'
	mapCleanUp(outFilename)
	out = ' >> '+outFilename
	gmt= os.popen('psxy -B2WESn -P -Ey -JX6i/-6i -R'+xyregion+' -Cno_green_25_50.cpt -St.2i -G0 -K '+out, 'w')
	results = csv.reader(open(dataFile, 'r'))
	mean = 0.0
	numMean = 0
	for row in results:
	    if row[0].startswith('#'):
		continue
	    if onlyOneNet != '' and onlyOneNet != row[0]:
		continue
	    if row[5] == '' or  row[5] == '...':
		row[5] = '59'
		row[6] = '0'
		row[12] = -1
	    thick = float(row[5].replace(' km','')) 
	    thickErr = float(row[6].replace(' km','')) 
	    stalat = float(row[2])
	    if stalat >= lat and stalat < (lat+incLat):
		if int(row[12]) >= minEQ:
		    mean = mean +float(thick)
		    numMean = numMean+1
		    gmt.write( "%s %f %f %f %f\n" % (row[3], thick, thick, thickErr, thickErr)) 
	gmt.close()
	if numMean > 0 :
	    mean = mean / numMean
	print str(lat)+' mean='+str(mean)+' numMean='+str(numMean)
# label
	gmt= os.popen('pstext -P -JX -R -O -K '+out, 'w')
	results = csv.reader(open(dataFile, 'r'))
	mean = 0.0
	numMean = 0
	for row in results:
	    if row[0].startswith('#'):
		continue
	    if onlyOneNet != '' and onlyOneNet != row[0]:
		continue
	    thick = row[5].replace(' km','') 
	    if row[5] == '' or  row[5] == '...':
		row[5] = 59
		row[12] = '-1'
	    stalat = float(row[2])
	    #print "%s.%s %s" % (row[0],row[1],row[12])
	    if stalat >= lat and stalat < (lat+incLat):
		if int(row[12]) >= minEQ:
		    gmt.write(row[3]+" "+thick+" 8 0 4 BL ."+row[0]+'.'+row[1]+"\n")
	gmt.close()	

	mapRegion='-124/-114/'+str(lat)+'/'+str(lat+incLat)
	makeMap(dataFile, outFilename, 'M6i', mapRegion,'-Y6.5i','-O', minEQ=minEQ, labelSta=mapLabelSta, onlyOneNet=onlyOneNet)
	ps2pdf(outFilename)

	
ears.gmtUseNegDegree()

#outFilename = 'mapTA.ps'
#ears.mapCleanUp(outFilename)
#makeMap('TAresult.csv', outFilename, 'M6i','-124/-114/32/49', onlyOneNet='TA' , minEQ=5)
#ears.ps2pdf( outFilename)

makeBigMap('allWestCoast.csv', 'mapAll.ps', 'M4i','-126/-115/32/50', minEQ=10, maxComplexity=.75 ,labelSta=True)
ears.ps2pdf('mapAll.ps')

#makeLonVsThickBoxes('allWestCoast.csv', 'lonBox_', 32, 49, 1, minEQ=5)

# grd2cpt /data/GMT/Grid/gtopo30/w140n40.grd -Crelief -Z > topo.cpt
# makecpt -I -T25/50/1 -Cno_green > no_green_25_50


#makeBigMap('allWestCoast.csv', 'topoAll.ps', 'M5.5i','-126/-114/32/49', minEQ=5)

#makeLonVsThickBoxes('TAresult.csv', 'TAlonBox_', 32, 49, 2, onlyOneNet='TA' , minEQ=5, mapLabelSta=True)


#makeLonVsThickBoxes('allWestCoast.csv', 'topolonBox_', 32, 49, 1, minEQ=5, backgroundGRD='/data/GMT/Grid/gtopo30', cptFile='topo.cpt')
