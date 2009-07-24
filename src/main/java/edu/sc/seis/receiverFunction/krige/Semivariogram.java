package edu.sc.seis.receiverFunction.krige;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

import edu.iris.Fissures.network.NetworkIdUtil;
import edu.iris.Fissures.network.StationIdUtil;
import edu.iris.Fissures.network.StationImpl;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.hibernate.HibernateUtil;
import edu.sc.seis.fissuresUtil.hibernate.NetworkDB;
import edu.sc.seis.fissuresUtil.simple.Initializer;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.sod.hibernate.SodDB;


public class Semivariogram {
    
    public Semivariogram(float binWidthDeg, List<SumHKStack> results) {
        this.results = results;
        this.binWidthDeg = binWidthDeg;
    }
    
    void process() {
        logger.info("being process "+results.size());
        List<StationPairResult> pairList = new ArrayList<StationPairResult>();
        for (SumHKStack stack : results) {
            StationImpl sta = getStation(stack);
            staMap.put(stack, sta);
            if (stack.getHVariance() < .00001) {
                System.out.println(NetworkIdUtil.toStringNoDates(stack.getNet())+"."+stack.getStaCode()+" ahvar=  "+stack.getHVariance());
            }
        }
        for (int i = 0; i < results.size()-1; i++) {
            for (int j = i+1; j < results.size(); j++) {
                StationPairResult pair = new StationPairResult(results.get(i), results.get(j), staMap);
                if (pair.getDist() < MAX_PAIR_DISTANCE) {
                    pairList.add(pair);
                }
            }
        }
        logger.info("before dist sort");
        Collections.sort(pairList, new Comparator<StationPairResult>() {
            public int compare(StationPairResult o1, StationPairResult o2) {
                if (o1.getDist() == o2.getDist()) {return 0;}
                return o1.getDist() > o2.getDist() ? 1 : -1;
            }
        });
        double[] hBin = new double[(int)Math.ceil(MAX_PAIR_DISTANCE/binWidthDeg)];
        double[] binCount = new double[hBin.length];
        int curBin = 0;
        double globalVar = 0;
        for (StationPairResult pair : pairList) {
            globalVar += pair.getDeltaCrustalThickness();
            while (pair.getDist() > (curBin+1)*binWidthDeg) {
                curBin++;
            }
            double weight = 1.0/(pair.getA().getHVariance()+pair.getB().getHVariance());
            if (weight > 1) {weight = 1.0;}
            hBin[curBin] += pair.getDeltaCrustalThickness()*pair.getDeltaCrustalThickness()*weight;
            binCount[curBin]+=weight;
            //hBin[curBin] += pair.getDeltaCrustalThickness()*pair.getDeltaCrustalThickness();
            //binCount[curBin]++;
        }
        globalVar /= pairList.size();
        System.out.println("Semivariogram: "+binWidthDeg+" "+MAX_PAIR_DISTANCE+" "+globalVar+" "+pairList.size());
        for (int i = 0; i < hBin.length; i++) {
            if (binCount[i] != 0) {
                hBin[i] /= binCount[i];
                
                System.out.println((i*binWidthDeg)+" "+Math.sqrt(hBin[i])+" "+binCount[i]);
            }
        }
    }

    
    static StationImpl getStation(SumHKStack s) {
        NetworkDB netdb = NetworkDB.getSingleton();
        List<StationImpl> staList = netdb.getStationByCodes(s.getNet().get_code(), s.getStaCode());
        if (staList.size() == 0) {
            throw new RuntimeException("Shouldn't happen, no station in db for a SumHKStack");
        }
        return staList.get(0);
    }
    
    public static void main(String[] args) throws IOException {
        Properties props = Initializer.loadProperties(args);
        PropertyConfigurator.configure(props);
        RecFuncDB.setDataLoc(props.getProperty("cormorant.servers.ears.dataloc", RecFuncDB.getDataLoc()));
        ConnMgr.setDB(ConnMgr.POSTGRES);
        ConnMgr.setURL(props.getProperty("fissuresUtil.database.url"));
        HibernateUtil.setUpFromConnMgr(props, HibernateUtil.DEFAULT_EHCACHE_CONFIG);
        synchronized(HibernateUtil.class) {
            SodDB.configHibernate(HibernateUtil.getConfiguration());
            RecFuncDB.configHibernate(HibernateUtil.getConfiguration());
        }
        logger.info("connecting to database: " + ConnMgr.getURL());
        Semivariogram svgram = new Semivariogram(.5f, RecFuncDB.getSingleton().getAllSumStack(2.5f));
        svgram.process();
    }
    
    public static final float MAX_PAIR_DISTANCE = 10;
    
    float binWidthDeg;
    
    List<SumHKStack> results;
    
    Map<SumHKStack, StationImpl> staMap = new HashMap<SumHKStack, StationImpl>();
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Semivariogram.class);
}