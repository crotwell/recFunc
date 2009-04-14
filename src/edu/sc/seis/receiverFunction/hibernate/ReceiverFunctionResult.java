package edu.sc.seis.receiverFunction.hibernate;

import java.io.File;
import java.sql.Timestamp;

import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.fissuresUtil.bag.DistAz;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.fissuresUtil.hibernate.ChannelGroup;
import edu.sc.seis.fissuresUtil.sac.SacToFissures;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.seisFile.sac.SacTimeSeries;
import edu.sc.seis.sod.SodConfig;
import edu.sc.seis.sod.velocity.event.VelocityEvent;

public class ReceiverFunctionResult {

    // hibernate
    protected ReceiverFunctionResult() {}

    protected ReceiverFunctionResult(CacheEvent event,
                                     ChannelGroup channelGroup,
                                     float radialMatch,
                                     int radialBump,
                                     float transverseMatch,
                                     int transverseBump,
                                     float gwidth,
                                     int maxBumps,
                                     float tol,
                                     SodConfig config) {
        this.event = event;
        this.channelGroup = channelGroup;
        this.radialMatch = radialMatch;
        this.radialBump = radialBump;
        this.transverseMatch = transverseMatch;
        this.transverseBump = transverseBump;
        this.gwidth = gwidth;
        this.maxBumps = maxBumps;
        this.tol = tol;
        this.sodConfig = config;
        this.insertTime = ClockUtil.now().getTimestamp();
    }

    public ReceiverFunctionResult(CacheEvent event,
                                  ChannelGroup channelGroup,
                                  String originalFile1,
                                  String originalFile2,
                                  String originalFile3,
                                  String radialFile,
                                  String transverseFile,
                                  float radialMatch,
                                  int radialBump,
                                  float transverseMatch,
                                  int transverseBump,
                                  float gwidth,
                                  int maxBumps,
                                  float tol,
                                  SodConfig config) {
        this(event,
             channelGroup,
             radialMatch,
             radialBump,
             transverseMatch,
             transverseBump,
             gwidth,
             maxBumps,
             tol,
             config);
        this.originalFile1 = originalFile1;
        this.originalFile2 = originalFile2;
        this.originalFile3 = originalFile3;
        this.radialFile = radialFile;
        this.transverseFile = transverseFile;
    }


    public ReceiverFunctionResult(CacheEvent event,
                                  ChannelGroup channelGroup,
                                  LocalSeismogramImpl original1,
                                  LocalSeismogramImpl original2,
                                  LocalSeismogramImpl original3,
                                  LocalSeismogramImpl radial,
                                  LocalSeismogramImpl transverse,
                                  float radialMatch,
                                  int radialBump,
                                  float transverseMatch,
                                  int transverseBump,
                                  float gwidth,
                                  int maxBumps,
                                  float tol,
                                  SodConfig config) {
        this(event,
             channelGroup,
             radialMatch,
             radialBump,
             transverseMatch,
             transverseBump,
             gwidth,
             maxBumps,
             tol,
             config);
        this.original1 = original1;
        this.original2 = original2;
        this.original3 = original3;
        this.radial = radial;
        this.transverse = transverse;
    }
    public int getDbid() {
        return dbid;
    }

    public CacheEvent getEvent() {
        return event;
    }
    
    public VelocityEvent getVelocityEvent() {
        return new VelocityEvent(getEvent());
    }
    
    public DistAz getDistAz() {
        return new DistAz(getChannelGroup().getStation(), getEvent());
    }

    public ChannelGroup getChannelGroup() {
        return channelGroup;
    }

    public String getOriginalFile1() {
        return originalFile1;
    }

    public LocalSeismogramImpl getOriginal1() {
        if(original1 == null) {
            File stationDir = RecFuncDB.getDir(getEvent(),
                                                      getChannelGroup().getChannel1(),
                                                      getGwidth());
            try {
                SacTimeSeries sac = new SacTimeSeries();
                sac.read(new File(stationDir, getOriginalFile1()));
                LocalSeismogramImpl seis = SacToFissures.getSeismogram(sac);
                seis.channel_id = getChannelGroup().getChannel1().get_id();
                original1 = seis;
            } catch(Exception e) {
                throw new RuntimeException("Unable to load seismogram from file: "
                                                   + stationDir
                                                   + "/"
                                                   + getOriginalFile1(),
                                           e);
            }
        }
        return original1;
    }

    public String getOriginalFile2() {
        return originalFile2;
    }

    public LocalSeismogramImpl getOriginal2() {
        if(original2 == null) {
            File stationDir = RecFuncDB.getDir(getEvent(),
                                                      getChannelGroup().getChannel2(),
                                                      getGwidth());
            try {
                SacTimeSeries sac = new SacTimeSeries();
                sac.read(new File(stationDir, getOriginalFile2()));
                LocalSeismogramImpl seis = SacToFissures.getSeismogram(sac);
                seis.channel_id = getChannelGroup().getChannel2().get_id();
                original2 = seis;
            } catch(Exception e) {
                throw new RuntimeException("Unable to load seismogram from file: "
                                                   + stationDir
                                                   + "/"
                                                   + getOriginalFile1(),
                                           e);
            }
        }
        return original2;
    }

    public String getOriginalFile3() {
        return originalFile3;
    }

    public LocalSeismogramImpl getOriginal3() {
        if(original3 == null) {
            File stationDir = RecFuncDB.getDir(getEvent(),
                                                      getChannelGroup().getChannel3(),
                                                      getGwidth());
            try {
                SacTimeSeries sac = new SacTimeSeries();
                sac.read(new File(stationDir, getOriginalFile3()));
                LocalSeismogramImpl seis = SacToFissures.getSeismogram(sac);
                seis.channel_id = getChannelGroup().getChannel3().get_id();
                original3 = seis;
            } catch(Exception e) {
                throw new RuntimeException("Unable to load seismogram from file: "
                                                   + stationDir
                                                   + "/"
                                                   + getOriginalFile1(),
                                           e);
            }
        }
        return original3;
    }

    public String getRadialFile() {
        return radialFile;
    }

    public String getTransverseFile() {
        return transverseFile;
    }

    public float getRadialMatch() {
        return radialMatch;
    }

    public int getRadialBump() {
        return radialBump;
    }

    public float getTransverseMatch() {
        return transverseMatch;
    }

    public int getTransverseBump() {
        return transverseBump;
    }

    public float getGwidth() {
        return gwidth;
    }

    public int getMaxBumps() {
        return maxBumps;
    }

    public float getTol() {
        return tol;
    }

    public SodConfig getSodConfig() {
        return sodConfig;
    }

    protected void setDbid(int dbid) {
        this.dbid = dbid;
    }

    protected void setEvent(CacheEvent event) {
        this.event = event;
    }

    protected void setChannelGroup(ChannelGroup channelGroup) {
        this.channelGroup = channelGroup;
    }

    protected void setOriginalFile1(String originalFile1) {
        this.originalFile1 = originalFile1;
    }

    protected void setOriginalFile2(String originalFile2) {
        this.originalFile2 = originalFile2;
    }

    protected void setOriginalFile3(String originalFile3) {
        this.originalFile3 = originalFile3;
    }

    protected void setRadialFile(String radialFile) {
        this.radialFile = radialFile;
    }

    protected void setTransverseFile(String transverseFile) {
        this.transverseFile = transverseFile;
    }

    protected void setRadialMatch(float radialMatch) {
        this.radialMatch = radialMatch;
    }

    protected void setRadialBump(int radialBump) {
        this.radialBump = radialBump;
    }

    protected void setTransverseMatch(float transverseMatch) {
        this.transverseMatch = transverseMatch;
    }

    protected void setTransverseBump(int transverseBump) {
        this.transverseBump = transverseBump;
    }

    protected void setGwidth(float gwidth) {
        this.gwidth = gwidth;
    }

    protected void setMaxBumps(int maxBumps) {
        this.maxBumps = maxBumps;
    }

    protected void setTol(float tol) {
        this.tol = tol;
    }

    protected void setSodConfig(SodConfig config) {
        this.sodConfig = config;
    }

    public LocalSeismogramImpl getRadial() {
        if(radial == null) {
            File stationDir = RecFuncDB.getDir(getEvent(),
                                                      getChannelGroup().getChannel1(),
                                                      getGwidth());
            try {
                SacTimeSeries itrSAC = new SacTimeSeries();
                itrSAC.read(new File(stationDir, getRadialFile()));
                LocalSeismogramImpl itrSeis = SacToFissures.getSeismogram(itrSAC);
                itrSeis.y_unit = UnitImpl.DIMENSONLESS;
                radial = itrSeis;
            } catch(Exception e) {
                throw new RuntimeException("Unable to load seismogram from file: "
                                                   + stationDir
                                                   + "/"
                                                   + getTransverseFile(),
                                           e);
            }
        }
        return radial;
    }

    public void setRadial(LocalSeismogramImpl radial) {
        this.radial = radial;
    }

    public LocalSeismogramImpl getTransverse() {
        if(transverse == null) {
            File stationDir = RecFuncDB.getDir(getEvent(),
                                                      getChannelGroup().getChannel1(),
                                                      getGwidth());
            try {
                SacTimeSeries itrSAC = new SacTimeSeries();
                itrSAC.read(new File(stationDir, getTransverseFile()));
                LocalSeismogramImpl itrSeis = SacToFissures.getSeismogram(itrSAC);
                itrSeis.y_unit = UnitImpl.DIMENSONLESS;
                transverse = itrSeis;
            } catch(Exception e) {
                throw new RuntimeException("Unable to load seismogram from file: "
                                                   + stationDir
                                                   + "/"
                                                   + getTransverseFile(),
                                           e);
            }
        }
        return transverse;
    }

    public void setTransverse(LocalSeismogramImpl transverse) {
        this.transverse = transverse;
    }

    public Timestamp getInsertTime() {
        return insertTime;
    }

    protected void setInsertTime(Timestamp insertTime) {
        this.insertTime = insertTime;
    }

    public RecFuncQCResult getQc() {
        return qc;
    }

    public void setQc(RecFuncQCResult qc) {
        this.qc = qc;
    }

    public HKStack getHKstack() {
        if (hkstack != null) {
            hkstack.setGaussianWidth(getGwidth());
        }
        return hkstack;
    }

    public void setHKstack(HKStack hkstack) {
        this.hkstack = hkstack;
    }

    int dbid;

    CacheEvent event;

    ChannelGroup channelGroup;

    String originalFile1;

    transient LocalSeismogramImpl original1;

    String originalFile2;

    transient LocalSeismogramImpl original2;

    String originalFile3;

    transient LocalSeismogramImpl original3;

    String radialFile;

    transient LocalSeismogramImpl radial;

    String transverseFile;

    transient LocalSeismogramImpl transverse;

    float radialMatch;

    int radialBump;

    float transverseMatch;

    int transverseBump;

    float gwidth;

    int maxBumps;

    float tol;

    SodConfig sodConfig;

    Timestamp insertTime;

    RecFuncQCResult qc;

    HKStack hkstack;
}
