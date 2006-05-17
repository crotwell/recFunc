#! /usr/bin/python

import csv, os, time

def mapCleanUp(outFilename):
    try:
	os.remove(outFilename)
    except OSError:
	pass
    try:
	os.remove(outFilename.replace('.ps', '.pdf'))
    except OSError:
	pass


def makeBigMap(dataFile, outFilename, proj='M5.5i', region='-126/-114/30/50', shift='', extra=''):
    mapCleanUp(outFilename)
    makeMap(dataFile, outFilename, proj, region, shift, extra)
    ps2pdf(outFilename)

def makeMap(dataFile, outFilename, proj='M5.5i', region='-126/-114/30/50', shift='', extra=''):
    out = ' >> '+outFilename
    gmt = os.popen('gmtset BASEMAP_TYPE=plain')
    gmt.close()
    baseGmtArgs = '-J'+proj+' -R'+region+' -P -K '+extra
    print 'baseGmtArgs='+baseGmtArgs
    gmt= os.popen('psbasemap ' +baseGmtArgs+' -B2 '+shift+'  '+out, 'w')
    gmt.close()
    if baseGmtArgs.find('-O') == -1:
	print 'adding -O'
	baseGmtArgs = baseGmtArgs + ' -O '

    # makecpt -I -T25/50/1 -Cno_green > no_green_25_50
    gmt = os.popen('grdimage ../crust2_180.grd -T -Cno_green_25_50 ' +baseGmtArgs+ out, 'w')
    gmt.close()

    gmt= os.popen('pscoast ' +baseGmtArgs+' -Dh -Na -W '+out, 'w')
    gmt.close()

    gmt= os.popen('psxy '+baseGmtArgs+' -St.2i -Cno_green_25_50 -W'+out, 'w')
    results = csv.reader(open(dataFile, 'r'))
    mean = 0.0
    numMean = 0
    for row in results: 
	if row[0].startswith('#') or  row[5] == '' or  row[5] == '...':
	    continue
	thick = row[5].replace(' km','')
	if ( float(thick) > 50):
	    #print row[0]+' '+ row[1]+" "+ row[3]+"  "+ row[2]+"  "+row[5]
	    pass
	if int(row[12]) >= 5 and thick != '...':
	    mean = mean +float(thick)
	    numMean = numMean+1
	    gmt.write(row[3]+"  "+ row[2]+"  "+thick+"\n")
	else:
	    gmt.write(row[3]+"  "+ row[2]+"  NaN\n") 
    gmt.close()
    mean = mean / numMean
    print 'mean='+str(mean)+' numMean='+str(numMean)
    gmt = os.popen('psscale -D6i/7i/5i/.5i -Cno_green_25_50 -B5 -P -O -K '+out, 'w')
    gmt.close()

    baseGmtArgs = baseGmtArgs.replace('-K', '')
    gmt= os.popen('psxy '+baseGmtArgs+' -St.003i -G255 '+out, 'w')
    gmt.write("0  -90\n")
    gmt.close()


def makeLonVsThickBoxes(dataFile, outfilePrefix, minLat, maxLat, incLat):
    xyregion='-126/-114/20/60'
    for lat in range(minLat, maxLat, incLat):
	outFilename = outfilePrefix+str(lat)+'.ps'
	mapCleanUp(outFilename)
	out = ' >> '+outFilename
	gmt= os.popen('psxy -B2WESn -P -JX6i/-6i -R'+xyregion+' -St.2i -G0 -K '+out, 'w')
	results = csv.reader(open(dataFile, 'r'))
	mean = 0.0
	numMean = 0
	for row in results:
	    if row[0].startswith('#') or  row[5] == '' or  row[5] == '...':
		continue
	    thick = row[5].replace(' km','') 
	    stalat = float(row[2])
	    if  int(row[12]) >= 5 and stalat > lat and stalat < (lat+incLat):
		mean = mean +float(thick)
		numMean = numMean+1
		gmt.write(row[3]+"  "+thick+" "+row[7]+"\n") 
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
	    if row[0].startswith('#') or  row[5] == '' or  row[5] == '...':
		continue
	    thick = row[5].replace(' km','') 
	    stalat = float(row[2])
	    print "%s.%s %s" % (row[0],row[1],row[12])
	    if int(row[12]) >= 5 and stalat > lat and stalat < (lat+incLat):
		gmt.write(row[3]+" "+thick+" 8 0 4 BL ."+row[0]+'.'+row[1]+"\n")
	gmt.close()	

	mapRegion='-126/-114/'+str(lat)+'/'+str(lat+incLat)
	makeMap(dataFile, outFilename, 'M6i', mapRegion,'-Y6.5i','-O')
	ps2pdf(outFilename)
	    
def ps2pdf(outFilename):
    ps2pdf = os.popen('ps2pdf '+outFilename)
    ps2pdf.close()
    try:
	os.remove(outFilename)
	pass
    except OSError:
	pass
	
gmtset = os.popen('gmtset OUTPUT_DEGREE_FORMAT=-ddd PLOT_DEGREE_FORMAT=-ddd')
gmtset.close()

outFilename = 'mapTA.ps'
#makeMap('TA.result', outFilename)

makeBigMap('allWestCoast.csv', 'mapAll.ps', 'M5.5i','-126/-114/36/38' )
#ps2pdf('mapAll.ps')

makeLonVsThickBoxes('allWestCoast.csv', 'lonBox_', 32, 42, 2)

makeLonVsThickBoxes('TAresult.csv', 'TAlonBox_', 32, 42, 2)
