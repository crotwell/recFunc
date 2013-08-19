#! /usr/bin/python

import csv, os, time, math
from distaz import DistAz

def ps2pdf(outFilename):
    ps2pdf = os.popen('pstopdf '+outFilename)
    ps2pdf.close()
    try:
	os.remove(outFilename)
	pass
    except OSError:
	pass

def makeMagVsDist(outFilename, dataFile):
    out = ' >> '+outFilename
    #gmt= os.popen('psxy -B2WESn -P -JX6i/-6i -R20/100/5/10 -Sp.05i -G0 -K '+out, 'w')
    dialect = csv.get_dialect('excel')
    dialect.delimiter = '|'
    distMagBin = {}
    results = csv.reader(open(dataFile, 'r'), dialect=dialect)
    row = results.next()
    row = results.next()
    for row in results:
	distaz = DistAz(float(row[4]), float(row[5]), float(row[2]), float(row[3]))
	distBin = 5+math.ceil(distaz.getDelta()/10)*10
	magBin = round(float(row[7])*10)/10.0
	distMagBin["%i %f" % (distBin,magBin)] = distMagBin.setdefault("%i %f" % (distBin,magBin), 0) + 1
#	gmt.write("%f %s\n" % (distaz.getDelta(), row[7]))
#    gmt.close()

    gmt = os.popen('psxyz -R30/100/5/9/1/1000 -P -JX6.5 -JZ2.5i -So0.3ib1 -Ggray -W0.5p  -E150/50 -B10/1/20:"Num Eq for Dist, Mag":WSneZ'+out, 'w')
    for key in distMagBin:
	gmt.write("%s %i\n" % (key, distMagBin[key]))
    gmt.close()

makeMagVsDist('magDistZ.ps', 'originRFPairs.out')
ps2pdf('magDistZ.ps')
