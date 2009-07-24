package edu.sc.seis.receiverFunction.web;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.StationImpl;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.hibernate.NetworkDB;
import edu.sc.seis.fissuresUtil.simple.TimeOMatic;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.crust2.Crust2;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.velocity.network.VelocityStation;

/**
 * @author crotwell Created on Mar 30, 2005
 */
public class ComparePriorResult extends StationList {

    public ComparePriorResult() throws ConfigurationException, Exception {
        crust2 = new Crust2();
    }

    public static String getReqName(HttpServletRequest req) {
        return RevUtil.get("name", req, "crust2.0");
    }

    public synchronized ArrayList getStations(HttpServletRequest req,
                                              RevletContext context)
            throws SQLException, NotFound {
        TimeOMatic.start();
        ArrayList stations = new ArrayList();
        String name = getReqName(req);
        context.put("name", name);
        float hDiff = RevUtil.getFloat("hDiff", req, -1);
        if(hDiff != -1) {
            context.put("hDiff", RevUtil.get("hDiff", req));
        }
        HashMap prior = new HashMap();
        context.put("prior", prior);
        HashMap hDiffMap = new HashMap();
        context.put("hDiffMap", hDiffMap);
        if(name.equals("crust2.0") || name.equals("Crust2.0")) {
            context.put("ref", Crust2.getReference());
        } else {
            context.put("ref", RecFuncDB.getSingleton()
                    .getPriorResultsRef(name));
        }
        if(cache.containsKey(name)) {
            TimedCacheItem cachedItem = (TimedCacheItem)cache.get(name);
            if(cachedItem.stations != null
                    && ClockUtil.now()
                            .subtract(cachedItem.when)
                            .lessThan(SummaryCache.CACHE_TIME)) {
                context.put("prior", cachedItem.prior);
                return cachedItem.stations;
            }
        }
        List<StationResult> results;
        if(name.equals("crust2.0") || name.equals("Crust2.0")) {
            StationImpl[] allsta = NetworkDB.getSingleton().getAllStations();
            for(int i = 0; i < allsta.length; i++) {
                VelocityStation station = new VelocityStation(allsta[i]);
                stations.add(station);
                ArrayList resultList = new ArrayList();
                resultList.add(crust2.getStationResult(station));
                prior.put(station, resultList);
            }
        } else {
            results = RecFuncDB.getSingleton().getPriorResults(name);
            for(StationResult stationResult : results) {
                List<StationImpl> staList = NetworkDB.getSingleton()
                .getStationForNet(stationResult.getNet(),
                                  stationResult.getStaCode());
                if (staList.size() == 0) {
                    continue;
                }
                VelocityStation station = new VelocityStation(staList.get(0));
                boolean found = false;
                for(Iterator iter = stations.iterator(); iter.hasNext();) {
                    VelocityStation sta = (VelocityStation)iter.next();
                    if(sta.getNetCode().equals(station.getNetCode())
                            && sta.get_code().equals(station.get_code())) {
                        // station already in list, use prior station
                        found = true;
                        station = sta;
                        break;
                    }
                }
                if(!found) {
                    stations.add(station);
                    Collections.sort(stations, new StationAlpha());
                }
                if(!prior.containsKey(station)) {
                    prior.put(station, new ArrayList());
                }
                ArrayList resultList = (ArrayList)prior.get(station);
                resultList.add(stationResult);
            }
        }
        TimedCacheItem item = new TimedCacheItem(ClockUtil.now(),
                                                 name,
                                                 stations,
                                                 null,
                                                 prior);
        cache.put(name, item);
        TimeOMatic.print("getStations");
        return stations;
    }

    public synchronized HashMap getSummaries(ArrayList stationList,
                                             RevletContext context,
                                             HttpServletRequest req)
            throws SQLException, IOException, NotFound {
        TimeOMatic.start();
        String name = getReqName(req);
        if(cache.containsKey(name)) {
            TimedCacheItem cachedItem = (TimedCacheItem)cache.get(name);
            if(cachedItem.summaries != null
                    && ClockUtil.now()
                            .subtract(cachedItem.when)
                            .lessThan(SummaryCache.CACHE_TIME)) {
                return cachedItem.summaries;
            }
        }
        // clean station/prior results if they agree within hDiff km
        HashMap summary = super.getSummaries(stationList, context, req);
        cleanSummaries(stationList, summary);
        HashMap prior = (HashMap)context.get("prior");
        HashMap hDiffMap = new HashMap();
        context.put("hDiffMap", hDiffMap);
        float hDiff;
        if(context.containsKey("hDiff")) {
            hDiff = Float.parseFloat((String)context.get("hDiff"));
        } else {
            hDiff = -1;
        }
        Iterator it = stationList.iterator();
        while(it.hasNext()) {
            VelocityStation station = (VelocityStation)it.next();
            ArrayList resultList = (ArrayList)prior.get(station);
            Iterator resultListIterator = resultList.iterator();
            boolean remove = false;
            while(resultListIterator.hasNext()) {
                StationResult result = (StationResult)resultListIterator.next();
                SumHKStack sumStack = (SumHKStack)summary.get(station);
                if(result == null || sumStack == null) {
                    remove = true;
                } else {
                    double diff = sumStack.getSum()
                            .getMaxValueH()
                            .subtract(result.getH())
                            .getValue(UnitImpl.KILOMETER);
                    if(hDiff > 0 && Math.abs(diff) < hDiff) {
                        remove = true;
                    } else {
                        logger.debug("Add diff of " + diff
                                + " to hDiffMap for " + station);
                        hDiffMap.put(result, "" + diff);
                    }
                }
            }
            if(remove) {
                it.remove();
                prior.remove(station);
            }
        }
        if(cache.containsKey(name)) {
            TimedCacheItem cachedItem = (TimedCacheItem)cache.get(name);
            cachedItem.summaries = summary;
            cache.put(name, cachedItem);
        }
        TimeOMatic.print("getSummaries");
        return summary;
    }

    public String getVelocityTemplate(HttpServletRequest req) {
        return "comparePriorResult.vm";
    }

    private HashMap cache = new HashMap();

    static Crust2 crust2 = null;

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ComparePriorResult.class);
}

class TimedCacheItem {

    MicroSecondDate when;

    String name;

    ArrayList stations;

    HashMap summaries, prior;

    public TimedCacheItem(MicroSecondDate when,
                          String name,
                          ArrayList stations,
                          HashMap summaries,
                          HashMap prior) {
        this.when = when;
        this.name = name;
        this.stations = stations;
        this.summaries = summaries;
        this.prior = prior;
    }
}

class StationAlpha implements Comparator {

    public int compare(Object o1, Object o2) {
        VelocityStation s1 = (VelocityStation)o1;
        VelocityStation s2 = (VelocityStation)o2;
        int netCompare = s1.getNetCode().compareTo(s2.getNetCode());
        if(netCompare != 0) {
            return s1.get_code().compareTo(s2.get_code());
        } else {
            return netCompare;
        }
    }
}