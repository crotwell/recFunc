#! /usr/bin/python -O
import sys, os, time
sys.path.append("../devTools/maven")
sys.path.append("./scripts")
sys.path.append("../sod/scripts")
import distBuilder, buildSodScripts, ProjectParser,  mavenExecutor

def buildDist(proj, name=None):
    curdir = os.path.abspath('.')
    os.chdir(proj.path)
    allProj = [ProjectParser.ProjectParser('../fissures/project.xml'),
               ProjectParser.ProjectParser('../fissuresUtil/project.xml'),
               ProjectParser.ProjectParser('../sod/project.xml'),
               proj]
    for otherProj in allProj: mavenExecutor.mavenExecutor(otherProj).jarinst()
    os.chdir(curdir)
    if not os.path.exists('scripts/logs'): os.mkdir('scripts/logs')
    extras = [('sodRF/scepp2003.xml', 'scepp2003.xml'),
              ('sodRF/Bolivia.xml', 'Bolivia.xml'),
              ('sodRF/irisRF.xml', 'irisRF.xml'),
              ('../sod/scripts/cwg.prop', 'cwg.prop'),
              ('scripts/logs', 'logs', False)]
    scripts = buildSodScripts.buildAll(proj)
    scriptsWithTarLoc = [(script, 'bin/'+script) for script in scripts]
    extras.extend(scriptsWithTarLoc)
    if name is None: name = proj.name + '-' + time.strftime('%y%m%d')
    distBuilder.buildDist(proj, extras, name)

if __name__ == "__main__":
    buildDist(ProjectParser.ProjectParser('./project.xml'))
