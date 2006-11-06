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
	rows.append(row)
    gmt= os.popen('psxy -B2WESn -P -JX6i/-6i -R-94/-70/25/60 -Sp.1i -G0/0/255 -K '+out, 'w')
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
	gmt.write(row[4]+"  "+ thick+" 12 90 4 BL ."+row[0]+'.'+row[1]+"\n")
	print(row[4]+"  "+ thick+" 12 90 4  BL ."+row[0]+'.'+row[1]+"\n")
    gmt.close()

makeHVsLon('momaH.ps', 'moma.csv')
ps2pdf('momaH.ps') 
