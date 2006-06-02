#! /usr/bin/python

import csv, os, time

def readData(dataFile, gmt, columns=[2,1,4], infinityVal=-1):
    results = csv.reader(open(dataFile, 'r'))
    mean=[0,0,0,0,0,0,0,0,0,0,0,0,0]
    num=[0,0,0,0,0,0,0,0,0,0,0,0,0]
    for row in results:
    	if row[0].startswith('#'):
	    continue
	line = ''
	for col in columns:
	    if row[col] == 'Infinity':
		row[col] = '-1'
	    else:
		mean[col] = mean[col] + float(row[col])
		num[col] = num[col]+1
		gmt.write(row[col]+' ')
		line = line + row[col]+' '
	gmt.write('\n')
	print(line)
	mean[col] = mean[col]/num[col]
	print 'mean[%i]=%f num=%f' % (col, mean[col], num[col])

def makeMap(dataFile, outFile):
    out = ' >> '+outFile
    xyRegion = '-126/-110/30/50'
    proj = '-JM5i'
    cptFile = 'rate.cpt'
    gmt= os.popen('pscoast -P -K '+proj+' -R' +xyRegion+' -Dh -Na -W '+out, 'w')
    gmt.close()

    gmt= os.popen('psxy -O -K -B2WESn -P '+proj+' -R'+xyRegion+' -C'+cptFile+' -St.2i -G0  '+out, 'w')
    readData(dataFile, gmt, [2, 1, 4])
    gmt.close()

    gmt = os.popen('psscale -D6i/7i/5i/.5i -C'+cptFile+' -B5 -P -O  '+out, 'w')
    gmt.close()

def makeHisto(dataFile, outFile, col=4, width=10):
    out = ' >> '+outFile
    gmt = os.popen('pshistogram -JX6i -W'+str(width)+' -G120 '+out, 'w')
    readData(dataFile, gmt, [col])
    gmt.close()
    
	    
def ps2pdf(outFilename):
    ps2pdf = os.popen('pstopdf '+outFilename)
    ps2pdf.close()
    try:
	os.remove(outFilename)
	pass
    except OSError:
	pass


gmtset = os.popen('gmtset OUTPUT_DEGREE_FORMAT=-ddd PLOT_DEGREE_FORMAT=-ddd')
gmtset.close()

datafile = 'taRate.csv'

#outFilename = 'mapTA.ps'
#makeMap(datafile, outFilename)
#ps2pdf(outFilename)


outFilename = 'histoTA_sPeriod.ps'
makeHisto(datafile, outFilename, 4, 5)
ps2pdf(outFilename)

outFilename = 'histoTA_tPeriod.ps'
makeHisto(datafile, outFilename, 5, 5)
ps2pdf(outFilename)

outFilename = 'histoTA_ratio.ps'
makeHisto(datafile, outFilename, 6, .02)
ps2pdf(outFilename)
