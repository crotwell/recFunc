package edu.sc.seis.receiverFunction.rest;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.IfNetwork.Station;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;
import edu.sc.seis.sod.velocity.network.VelocityStation;

public class Network extends Revlet {

    public Network() throws SQLException, IOException {
        this("jdbc:postgresql:ears", System.getProperty("user.home")
                + "/CacheServer/Ears/Data");
    }

    public Network(String databaseURL, String dataloc) throws SQLException, IOException {
        ConnMgr.setDB(ConnMgr.POSTGRES);
        ConnMgr.setURL(databaseURL);
        Connection conn = ConnMgr.createConnection();
        jdbcChannel = new JDBCChannel(conn);
    }

    protected void setContentType(HttpServletRequest request,
                                  HttpServletResponse response)
    {
        response.setContentType("text/xml");
    }
    
    public RevletContext getContext(HttpServletRequest req,
                                    HttpServletResponse res) throws Exception {
        String path = req.getPathInfo();
        if (path.endsWith(".xml")) {
            path = path.substring(0, path.indexOf(".xml"));
        }
        String[] parts = path.split("/");
        RevletContext c;
        if(parts[1].equals("test")) {
            c = new RevletContext("testNetwork.vm");
            c.put("net", parts[2]);
            c.put("sta", parts[3]);
            c.put("site", parts[4]);
            c.put("chan", parts[5]);
        } else if(parts.length == 2) {
            // assume network
            c = new RevletContext("restnetwork.vm");
            NetworkId[] nets = jdbcChannel.getStationTable().getNetTable().getByCode(parts[1]);
            Station[] stations = jdbcChannel.getStationTable().getAllStations(nets[0]);
            ArrayList stationList = new ArrayList();
            for(int i = 0; i < stations.length; i++) {
                stationList.add(new VelocityStation(stations[i]));
            }
            c.put("net", new VelocityNetwork(stationList));
        } else if(parts.length == 3) {
            // assume network
            c = new RevletContext("reststation.vm");
            c.put("net", parts[1]);
            c.put("sta", parts[2]);
        } else if(parts.length == 4) {
            // assume network
            c = new RevletContext("restsite.vm");
            c.put("net", parts[1]);
            c.put("sta", parts[2]);
            c.put("site", parts[3]);
        } else if(parts.length == 5) {
            // assume network
            c = new RevletContext("restchannel.vm");
            c.put("net", parts[1]);
            c.put("sta", parts[2]);
            c.put("site", parts[3]);
            c.put("chan", parts[4]);
        } else {
            c = new RevletContext("testNetwork.vm");
            c.put("net", parts[0]);
            c.put("sta", parts[1]);
            c.put("site", parts[2]);
            c.put("chan", parts[3]);
        }
        return c;
    }

    JDBCChannel jdbcChannel;
}
