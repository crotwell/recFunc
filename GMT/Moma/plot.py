#! /usr/bin/python

import csv, os

def ps2pdf(outFilename):
    os.popen('eps2eps '+outFilename+' '+outFilename.replace('.ps','.eps')).close()
    os.popen('epstopdf '+outFilename.replace('.ps','.eps')).close()
    try:
#	os.remove(outFilename)
	pass
    except OSError:
	pass

def plotGaussH(outFilename, gauss, color='0/0/0'):
    out = ' >> '+outFilename
    labelShift='1.25i'
    dialect = csv.get_dialect('excel')
    results = csv.reader(open('moma'+gauss+'.csv', 'r'), dialect=dialect)
    row = results.next()
    row = results.next()
    rows = []
    for row in results:
	rows.append(row)
#    gmt= os.popen('psxy -P -JX -R -G'+color+' -O -K '+out, 'w')
#    for row in rows:
#	gmt.write("%s %s\n" % (row[4], row[6].replace(' km','')))
#    gmt.close()
    gmt= os.popen('psxy -P -JX -R -Sc.08i -G'+color+' -O -K '+out, 'w')
    for row in rows:
        print "row %s %s\n" % (row[4], row[6].replace(' km',''))
	gmt.write("%s %s\n" % (row[4], row[6].replace(' km','')))
    gmt.close()
    gmt= os.popen('psxy -P -JX -R -St.1i -G255/0/0 -O -K '+out, 'w')
    for row in rows:
	gmt.write("%s %s\n" % (row[4], row[15].replace(' km','')))
    gmt.close()
    gmt= os.popen('pstext -Y'+labelShift+' -P -JX -R -O -K -W'+out, 'w')
    for row in rows:
	thick = row[5].replace(' km','')
	thick = '30'
	gmt.write(row[4]+"  "+ thick+" 12 90 4 ML "+row[0]+'.'+row[1]+"\n")
    gmt.close()
    os.popen('pstext -Y-'+labelShift+' -P -JX -R -O -K -W'+out, 'w').close()
    # plot stations without a moma result in array
    # also do pstext from array to get all stations
    results = csv.reader(open('moma'+gauss+'_nocomp.csv', 'r'), dialect=dialect)
    rows = []
    for row in results:
	rows.append(row)
    gmt= os.popen('pstext -Y'+labelShift+' -P -JX -R -O -K -W'+out, 'w')
    for row in rows:
	if (row[1] != 'MO18'):
	    thick = row[5].replace(' km','')
	    thick = '30'
	    gmt.write(row[3]+"  "+ thick+" 12 90 4 ML "+row[0]+'.'+row[1]+"\n")
    gmt.close()
    os.popen('pstext -Y-'+labelShift+' -P -JX -R -O -K -W'+out, 'w').close()
    gmt= os.popen('psxy -P -JX -R -Sc.08i -G'+color+' -O -K '+out, 'w')
    for row in rows:
	if row[5] != '':
	    gmt.write("%s %s\n" % (row[3], row[5]))
    gmt.close()

def legend(outFilename, offset, symbol, color, text):
    out = ' >> '+outFilename
    gmt=os.popen('psxy -P -JX -R -O -K -S'+symbol+' -G'+color+' '+out, 'w')
    gmt.write('-80 '+offset+' \n')
    gmt.close()
    gmt= os.popen('pstext -P -JX -R -O -K -W'+out, 'w')
    gmt.write('-79.5 '+offset+" 12 0 4 ML "+text+' \n')
    gmt.close()

def makeHVsLon(outFilename):
    out = ' >> '+outFilename
    gmt= os.popen('psxy -B2WESn -P -JX6i/-6i -R-94/-70/25/51 -K '+out, 'w')
    gmt.close()
    print 'aaaaaa'

    plotGaussH(outFilename, '2.5', '0/0/0')
    plotGaussH(outFilename, '1.0', '0/0/255')
#    plotGaussH(outFilename, '0.7', '0/255/0')
#    plotGaussH(outFilename, '0.4', '0/255/255')
#    gmt= os.popen('psxy -P -JX -R -O -K -Y2 '+out, 'w')
#    gmt.close()
    legend(outFilename, '26', 't.1i', '255/0/0', 'MOMA')
    legend(outFilename, '26.75', 'c.08i', '0/0/0', 'Gauss 2.5')
    legend(outFilename, '27.5', 'c.08i', '0/0/255', 'Gauss 1.0')
#    legend(outFilename, '28.25', 'c.08i', '0/255/0', 'Gauss 0.7')
#    legend(outFilename, '29', 'c.08i', '0/255/255', 'Gauss 0.4')

    gmt= os.popen('psxy -P -JX -R -O '+out, 'w')
    gmt.close()
    

makeHVsLon('momaH.ps')
ps2pdf('momaH.ps') 
