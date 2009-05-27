import urllib2, os


def doSta(netCode, staCode):
    try:
        outdir = 'HOPE'
        if not os.path.exists(outdir):
            os.mkdir(outdir)
        inData = None
        outFile = None
        inData = urllib2.urlopen('http://ears.seis.sc.edu/Data/Summary/gauss_2.5/%s/%s/SumHKStackTxt.xyz'%(netCode, staCode))
        line = inData.readline()

        outFile = open('%s/%s.%s.xyz'%('HOPE',netCode, staCode), 'w')
        outFile.write(line)
        outFile.write(inData.read())
        outFile.close()
        inData.close()
    except urllib2.HTTPError, inst:
            if not inData is None:
                inData.close()
            if not outFile is None: outFile.close()
            if inst.code == 404:
                print 'i404 - no stack for station %s.%s'%(netCode, staCode)
            else:
                print 'big problem: %s %s'%(type(inst) , inst)


def getDirs(url):
        out = []
	inData = urllib2.urlopen(url)
        for i in range(0,9):
                line = inData.readline()
        for line in inData:
                if line.find('<tr><td valign="top"><img src="/icons/folder.gif" alt="[DIR]"></td><td><a') != -1:
                        out.append(line.split()[4].split('"')[1].rstrip('/'))
        inData.close()
        return out


netList = getDirs('http://ears.seis.sc.edu/Data/Summary/gauss_2.5')
for netCode in netList:
    staList = getDirs('http://ears.seis.sc.edu/Data/Summary/gauss_2.5/%s'%(netCode,))
    for staCode in staList:
        print netCode, staCode
        doSta(netCode, staCode)


print 'done'
