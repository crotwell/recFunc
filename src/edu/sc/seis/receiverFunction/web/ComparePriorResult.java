package edu.sc.seis.receiverFunction.web;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.iris.Fissures.IfNetwork.Station;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.simple.TimeOMatic;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.compare.JDBCStationResult;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.crust2.Crust2;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.rev.velocity.VelocityStation;
import edu.sc.seis.sod.ConfigurationException;

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
                prior.put(station, crust2.getStationResult(station));
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
                    stations.add(station);
                    prior.put(station, results[i]);
                } catch (NotFound e) {
                    // this station is in the prior result, but not in ears, skip...
                }
            }
        }
        return stations;
    }
    
    public HashMap getSummaries(ArrayList stationList, RevletContext context) throws SQLException, IOException {
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
        return summary;
    }

    public String getVelocityTemplate() {
        return "comparePriorResult.vm";
    }
    
    static Crust2 crust2 = null;
    
    JDBCStationResult jdbcStationResult;
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ComparePriorResult.class);
}