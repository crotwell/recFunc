package edu.sc.seis.receiverFunction.web;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import edu.iris.Fissures.IfNetwork.Station;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.simple.TimeOMatic;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.compare.JDBCStationResult;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.crust2.Crust2;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.velocity.network.VelocityStation;

/**
 * @author crotwell Created on Mar 30, 2005
 */
public class ComparePriorResult extends StationList {

    public ComparePriorResult() throws SQLException, ConfigurationException,
            Exception {
        super();
        jdbcStationResult = new JDBCStationResult(jdbcChannel.getNetworkTable());
        crust2 = new Crust2();
    }
    
    public ComparePriorResult(String databaseURL, String dataloc)
            throws SQLException, ConfigurationException, Exception {
        super(databaseURL, dataloc);
        jdbcStationResult = new JDBCStationResult(jdbcChannel.getNetworkTable());
        crust2 = new Crust2();
    }

    public ArrayList getStations(HttpServletRequest req, RevletContext context)
            throws SQLException, NotFound {
        TimeOMatic.start();
        ArrayList stations = new ArrayList();
        String name = RevUtil.get("name", req);
        context.put("name", name);
        float hDiff = RevUtil.getFloat("hDiff", req, -1);
        if (hDiff != -1) {
            context.put("hDiff", RevUtil.get("hDiff", req));
        }
        HashMap prior = new HashMap();
        context.put("prior", prior);
        StationResult[] results;
        if (name.equals("crust2.0")) {
            Station[] allsta = jdbcChannel.getStationTable().getAllStations();
            context.put("ref", Crust2.getReference());
            for(int i = 0; i < allsta.length; i++) {
                VelocityStation station = new VelocityStation(allsta[i], jdbcChannel.getStationTable().getDBId(allsta[i].get_id()));
                stations.add(station);
                ArrayList resultList = new ArrayList();
                resultList.add(crust2.getStationResult(station));
                prior.put(station, resultList);
            }
        } else {
            results = jdbcStationResult.getAll(name);
            if (results.length != 0) {
                context.put("ref", results[0].getRef());
            }
            for(int i = 0; i < results.length; i++) {
                try {
                    int[] dbids = jdbcChannel.getStationTable().getDBIds(results[i].getNetworkId(), results[i].getStationCode());
                    VelocityStation station = new VelocityStation(jdbcChannel.getStationTable().get(dbids[0]));
                    boolean found = false;
                    for(Iterator iter = stations.iterator(); iter.hasNext();) {
                        VelocityStation sta = (VelocityStation)iter.next();
                        if (sta.getNetCode().equals(station.getNetCode()) && sta.get_code().equals(station.get_code())) {
                            // station already in list, use prior station
                            found = true;
                            station = sta;
                            break;
                        }
                    }
                    if (! found) {
                        stations.add(station);
                        Collections.sort(stations, new StationAlpha());
                    }
                    if ( ! prior.containsKey(station)) {
                        prior.put(station, new ArrayList());
                    }
                    ArrayList resultList = (ArrayList)prior.get(station);
                    resultList.add(results[i]);
                } catch (NotFound e) {
                    // this station is in the prior result, but not in ears, skip...
                }
            }
        }
        TimeOMatic.print("getStations");
        return stations;
    }
    
    public HashMap getSummaries(ArrayList stationList, RevletContext context) throws SQLException, IOException {
        TimeOMatic.start();
        // clean station/prior results if they agree within hDiff km
        HashMap summary = super.getSummaries(stationList, context);
        HashMap prior = (HashMap)context.get("prior");
        
        float hDiff;
        if (context.containsKey("hDiff")) {
            hDiff = Float.parseFloat((String)context.get("hDiff"));
        } else {
            hDiff = -1;
        }
        if (hDiff > 0) {
            Iterator it = stationList.iterator();
            while(it.hasNext()) {
                VelocityStation station = (VelocityStation)it.next();
                StationResult result = (StationResult)prior.get(station);
                SumHKStack sumStack = (SumHKStack)summary.get(station);
                boolean remove = false;
                if (result == null || sumStack == null) {
                    remove = true;
                } else {
                    double diff =sumStack.getSum().getMaxValueH().subtract(result.getH()).getValue(UnitImpl.KILOMETER); 
                    if (Math.abs(diff) < hDiff ) {
                        remove = true;
                    }
                }
                if (remove) {
                    it.remove();
                    prior.remove(station);
                }
                
            }
        }
        TimeOMatic.print("getSummaries");
        return summary;
    }

    public String getVelocityTemplate() {
        return "comparePriorResult.vm";
    }
    
    static Crust2 crust2 = null;
    
    JDBCStationResult jdbcStationResult;
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ComparePriorResult.class);
}

class StationAlpha implements Comparator {
    
    public int compare(Object o1, Object o2) {
        VelocityStation s1 = (VelocityStation)o1;
        VelocityStation s2 = (VelocityStation)o2;
        int netCompare = s1.getNetCode().compareTo(s2.getNetCode());
        if (netCompare  != 0) {
            return s1.get_code().compareTo(s2.get_code());
        } else {
            return netCompare;
        }
    }
}