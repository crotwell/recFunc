import sys, os, time
sys.path.append("../devTools/maven")
sys.path.append("./scripts")
sys.path.append("../sod/scripts")
import distBuilder, sodScriptBuilder, ProjectParser

def buildDist(proj, name=None):
    if not os.path.exists('scripts/logs'): os.mkdir('scripts/logs')
    extras = [('sodRF/recfuncsod.sh', 'recfuncsod.sh'),
              ('sodRF/scepp2003.xml', 'scepp2003.xml'),
              ('sodRF/Bolivia.xml', 'Bolivia.xml'),
              ('sodRF/irisRF.xml', 'irisRF.xml'),
              ('../sod/scripts/cwg.prop', 'cwg.prop'),
              ('scripts/logs', 'logs', False)]
    scripts = sodScriptBuilder.buildAll(proj)
    if name is None: name = proj.name + '-' + time.strftime('%y%m%d')
    distBuilder.buildDist(proj, True, extras, scripts, name)

if __name__ == "__main__":
    buildDist(ProjectParser.ProjectParser('./project.xml'))
