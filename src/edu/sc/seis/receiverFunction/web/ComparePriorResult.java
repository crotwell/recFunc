package edu.sc.seis.receiverFunction.web;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.receiverFunction.compare.JDBCStationResult;
import edu.sc.seis.receiverFunction.compare.StationResult;
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
    }
    
    public ComparePriorResult(String databaseURL, String dataloc)
            throws SQLException, ConfigurationException, Exception {
        super(databaseURL, dataloc);
        jdbcStationResult = new JDBCStationResult(jdbcChannel.getNetworkTable());
    }

    public ArrayList getStations(HttpServletRequest req, RevletContext context)
            throws SQLException, NotFound {
        ArrayList stations = new ArrayList();
        String name = RevUtil.get("name", req);
        context.put("name", name);
        StationResult[] results = jdbcStationResult.getAll(name);
        if (results.length != 0) {
            context.put("ref", results[0].getRef());
        }
        HashMap prior = new HashMap();
        context.put("prior", prior);
        for(int i = 0; i < results.length; i++) {
            int[] dbids = jdbcChannel.getStationTable().getDBIds(results[i].getNetworkId(), results[i].getStationCode());
            VelocityStation station = new VelocityStation(jdbcChannel.getStationTable().get(dbids[0]));
            stations.add(station);
            prior.put(station, results[i]);
        }
        return stations;
    }

    public String getVelocityTemplate() {
        return "comparePriorResult.vm";
    }
    
    JDBCStationResult jdbcStationResult;
}