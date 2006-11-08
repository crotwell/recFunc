#! /usr/bin/python

import csv, os

def ps2pdf(outFilename):
    ps2pdf = os.popen('pstopdf '+outFilename)
    ps2pdf.close()
    try:
	os.remove(outFilename)
	pass
    except OSError:
	pass


def makeHVsLon(outFilename, dataFile):
    out = ' >> '+outFilename
    dialect = csv.get_dialect('excel')
    results = csv.reader(open(dataFile, 'r'), dialect=dialect)
    row = results.next()
    row = results.next()
    rows = []
    for row in results:
	if (int(row[12]) > 5):
	    rows.append(row)
    gmt= os.popen('psxy -B2WESn -P -JX6i/-6i -R-111.0/-102.7/24/61 -Sp.08i -G0/0/255 -K '+out, 'w')
    for row in rows:
	gmt.write("%s %s\n" % (row[4], row[5]))
    gmt.close()
    gmt= os.popen('psxy -P -JX -R -St.1i -G255/0/0 -O -K '+out, 'w')
    for row in rows:
	gmt.write("%s %s\n" % (row[4], row[14]))
    gmt.close()
    gmt= os.popen('pstext -P -JX -R -O -W'+out, 'w')
    for row in rows:
	thick = row[5].replace(' km','')
	thick = '30'
	gmt.write(row[4]+"  "+ thick+" 12 90 4 BL "+row[0]+'.'+row[1]+"\n")
    gmt.close()

makeHVsLon('ristraH_5.ps', 'ristra.csv')
ps2pdf('ristraH_5.ps') 
