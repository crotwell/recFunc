#! /usr/bin/python -O
import sys, os, time
sys.path.append("../devTools/maven")
import ProjectParser, mavenExecutor, depCopy, distBuilder
sys.path.append("../sod/")
import build
from optparse import OptionParser

def buildJars(proj, clean=False):
    curdir = os.path.abspath('.')
    os.chdir(proj.path)
    allProj = [ProjectParser.ProjectParser('../fissures/project.xml'),
               ProjectParser.ProjectParser('../fissuresUtil/project.xml'),
               ProjectParser.ProjectParser('../sod/project.xml'),
               ProjectParser.ProjectParser('../gee/project.xml'),
               proj]
    if clean:
        for proj in allProj: mavenExecutor.mavenExecutor(proj).clean()
    compiled = False
    for proj in allProj:
        if mavenExecutor.mavenExecutor(proj).jarinst():
            compiled = True
    os.chdir(curdir)
    return compiled

def buildDist(proj, name, clean=False):
    buildJars(proj, clean)
    if not os.path.exists('scripts/logs'): os.mkdir('scripts/logs')
    extras = [('sodRF/scepp2003.xml', 'scepp2003.xml'),
              ('sodRF/Bolivia.xml', 'Bolivia.xml'),
              ('sodRF/irisRF.xml', 'irisRF.xml'),
              ('../sod/scripts/cwg.prop', 'cwg.prop'),
              ('scripts/logs', 'logs', False)]
    scripts = build.buildAllScripts(proj)
    scriptsWithTarLoc = [(script, 'bin/'+script) for script in scripts]
    extras.extend(scriptsWithTarLoc)
    distBuilder.buildDist(proj, extras, name)
    for script in scripts: os.remove(script)

def buildName(proj): return proj.name + '-' + time.strftime('%y%m%d')

if __name__ == "__main__":
    proj = ProjectParser.ProjectParser('./project.xml')
    parser = OptionParser()
    parser.add_option("-d", "--dist", dest="dist",
                      help="build tar dist",
                      default=False,
                      action="store_true")
    parser.add_option("-c", "--clean", dest="clean",
                      help="clean out previously compiled items",
                      default=False,
                      action="store_true")
    parser.add_option("-n", "--name", dest="name",
                      help="archive file name for use with -d", metavar="NAME",
                      default=buildName(proj))
    parser.add_option("-s", "--scripts", dest="scripts",
                      help="compile recFunc and build run scripts(default option)",
                      default=True,
                      action="store_true")
    options = parser.parse_args()[0]
    if options.dist : buildDist(proj, options.name, options.clean)
    else :
        buildJars(proj, options.clean)
        os.chdir('scripts')
        build.buildAllScripts(proj)
        depCopy.copy(proj)

