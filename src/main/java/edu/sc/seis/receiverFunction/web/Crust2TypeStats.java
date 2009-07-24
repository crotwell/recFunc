package edu.sc.seis.receiverFunction.web;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.fissuresUtil.bag.Statistics;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.crust2.Crust2Profile;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.velocity.network.VelocityStation;


public class Crust2TypeStats  extends Revlet {

    public Crust2TypeStats() throws SQLException, ConfigurationException,
            Exception {
        comparePriorResult = new ComparePriorResult();
    }
    
    public RevletContext getContext(HttpServletRequest req, HttpServletResponse res) throws Exception {
        int minEQ = RevUtil.getInt("minEQ",
                                      req,
                                      0);
        int maxEQ = RevUtil.getInt("maxEQ",
                                      req,
                                      99999);
        RevletContext context = new RevletContext("crust2TypeStats.vm",
                                                  Start.getDefaultContext());
        Revlet.loadStandardQueryParams(req, context);
        context.put("name","crust2.0");
        ArrayList stations = comparePriorResult.getStations(req, context);
        StationList.cleanStations(stations);
        HashMap summary = comparePriorResult.getSummaries(stations, context, req);
        comparePriorResult.cleanSummaries(stations, summary);
        float[] allDiffs = new float[stations.size()];
        HashMap typeDiffs = new HashMap();
        Iterator typeIterator = oneLetterCodes.keySet().iterator();
        while(typeIterator.hasNext()) {
            Object key = typeIterator.next();
            if (typeDiffs.get(key) == null) {
                typeDiffs.put(oneLetterCodes.get(key), new ArrayList());
            }
        }
        Iterator it = stations.iterator();
        while(it.hasNext()) {
            VelocityStation sta = (VelocityStation)it.next();
            SumHKStack sum = (SumHKStack)summary.get(sta);
            if (sum.getNumEQ() <= minEQ) {
                it.remove();
                summary.remove(sta);
            }
        }
        int i=0;
        it = stations.iterator();
        while(it.hasNext()) {
            VelocityStation sta = (VelocityStation)it.next();
            SumHKStack sum = (SumHKStack)summary.get(sta);
            if (sum.getNumEQ() < 3) {it.remove();} else {
            Crust2Profile profile = ComparePriorResult.crust2.getClosest(sta.getLocation().longitude, sta.getLocation().latitude);
            allDiffs[i] = (float)profile.getCrustThickness().getValue(UnitImpl.KILOMETER)-sum.getComplexityResult().getBestH();
            ArrayList typeDiffList = (ArrayList)typeDiffs.get(oneLetterCodes.get(profile.getCode().substring(0,1)));
            typeDiffList.add(new Float(allDiffs[i]));
            
            i++;
            }
        }
        HashMap numSamps = new HashMap();
        context.put("numSamps", numSamps);
        HashMap mean = new HashMap();
        context.put("mean", mean);
        HashMap stddev = new HashMap();
        context.put("stddev", stddev);
        Statistics allDiffStats = new Statistics(allDiffs);
        numSamps.put("all", ""+allDiffStats.getLength());
        mean.put("all", formatter.format(allDiffStats.mean()));
        stddev.put("all", formatter.format(allDiffStats.stddev()));
        
        typeIterator = oneLetterCodes.keySet().iterator();
        while(typeIterator.hasNext()) {
            Object key = typeIterator.next();
            ArrayList typeDiffList = (ArrayList)typeDiffs.get(oneLetterCodes.get(key));
            float[] floatDiffs = new float[typeDiffList.size()];
            for(int j = 0; j < floatDiffs.length; j++) {
                floatDiffs[j] = ((Float)typeDiffList.get(j)).floatValue();
            }
            Statistics diffStats = new Statistics(floatDiffs);
            numSamps.put(oneLetterCodes.get(key).toString(), ""+diffStats.getLength());
            mean.put(oneLetterCodes.get(key).toString(), formatter.format(diffStats.mean()));
            stddev.put(oneLetterCodes.get(key).toString(), formatter.format(diffStats.stddev()));
        }
        return context;
    }
    
    static DecimalFormat formatter = new DecimalFormat("#.##");
    
    static String ARCHEAN = "Archean";
    static String EM_PROTEROZOIC = "Early/Mid Proterozoic";
    static String LATE_PROTEROZOIC = "Late Proterozoic";
    static String PHANEROZOIC = "Phanerozoic";
    static String PLATFORM = "Precambrian Platform";
    static String EXTENDED_CRUST = "Extended Crust";
    static String OROGEN = "Orogen";
    static String FOREARC = "Forearc";
    static String CONT_ARC = "Continental Arc";
    static String SUBMARINE_PLATEAU = "Submarine Plateau";
    static String SHELF = "Continental Shelf";
    static String CONT_MARGIN = "Continental Margin";
    static String MELT_AFF_OCEAN = "Melt Affected Ocean";
    static String OCEAN = "Ocean";
    static String MARGIN_SHIELD_TRANS = "Margin /shield  transition";
    static String MARGIN_SHIELD = "Margin/Shield";
    static String RIFT = "Rift";
    static String ISLAND_ARC = "Island Arc";
    static String THINNED_CONT_CRUST = "thinned cont. crust/Black Sea/Caspian depression/Red Sea";
    
    static HashMap oneLetterCodes = new HashMap();
    
    static {
        oneLetterCodes.put("D", PLATFORM);
        oneLetterCodes.put("E", PLATFORM);
        oneLetterCodes.put("F", ARCHEAN);
        oneLetterCodes.put("G", ARCHEAN);
        oneLetterCodes.put("H", EM_PROTEROZOIC);
        oneLetterCodes.put("I", LATE_PROTEROZOIC);
        oneLetterCodes.put("K", FOREARC);
        oneLetterCodes.put("L", CONT_ARC);
        oneLetterCodes.put("M", EXTENDED_CRUST);
        oneLetterCodes.put("N", EXTENDED_CRUST);
        oneLetterCodes.put("O", OROGEN);
        oneLetterCodes.put("P", OROGEN);
        oneLetterCodes.put("Q", OROGEN);
        oneLetterCodes.put("R", OROGEN);
        oneLetterCodes.put("T", MARGIN_SHIELD_TRANS);
        oneLetterCodes.put("U", MARGIN_SHIELD);
        oneLetterCodes.put("X", RIFT);
        oneLetterCodes.put("Z", PHANEROZOIC);
        oneLetterCodes.put("A", OCEAN);
        oneLetterCodes.put("B", MELT_AFF_OCEAN);
        oneLetterCodes.put("C", SHELF);
        oneLetterCodes.put("S", CONT_MARGIN);
        oneLetterCodes.put("J", ISLAND_ARC);
        oneLetterCodes.put("W", SUBMARINE_PLATEAU);
        oneLetterCodes.put("Y", THINNED_CONT_CRUST);
    }
    
    ComparePriorResult comparePriorResult;
}
