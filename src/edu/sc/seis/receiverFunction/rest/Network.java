package edu.sc.seis.receiverFunction.rest;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.sun.rsasign.i;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.IfNetwork.NetworkAttr;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.IfNetwork.Station;
import edu.iris.Fissures.network.NetworkIdUtil;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.velocity.network.VelocityChannel;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;
import edu.sc.seis.sod.velocity.network.VelocityStation;

public class Network extends Revlet {

    public Network() throws SQLException, IOException {
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
        System.out.println("path: "+path);
        if (path == null) {path = "";}
        if (path.endsWith(".xml")) {
            path = path.substring(0, path.indexOf(".xml"));
        }
        String[] parts = path.split("/");
        if (parts == null) {parts = new String[] {""};}
        RevletContext c;
        if (parts.length == 1) {
            // list of networks
            c = new RevletContext("restnetworkList.vm");
            NetworkAttr[] nets = jdbcChannel.getNetworkTable().getAllNetworkAttrs();
            ArrayList list = new ArrayList();
            for(int i = 0; i < nets.length; i++) {
                list.add(new VelocityNetwork(nets[i]));
            }
            c.put("netList", list);
        } else if(parts.length == 2) {
            // assume network
            c = new RevletContext("restnetwork.vm");
            c.put("net", getNetwork(parts));
        } else if(parts.length == 3) {
            // assume station
            c = new RevletContext("reststation.vm");
            VelocityNetwork net = getNetwork(parts);
            VelocityStation sta = getStation(parts, net);
            ArrayList chanList = getAllChannels(parts, sta);
            c.put("net", net);
            c.put("sta", sta);
            c.put("channels", chanList);
        } else if(parts.length == 5) {
            // assume channel
            c = new RevletContext("restchannel.vm");
            VelocityNetwork net = getNetwork(parts);
            VelocityStation sta = getStation(parts, net);
            ArrayList chanList = getAllChannels(parts, sta);
            Iterator it = chanList.iterator();
            while (it.hasNext()) {
                VelocityChannel chan = (VelocityChannel)it.next();
                if (chan.my_site.get_code().equals(parts[3]) && chan.get_code().equals(parts[4])) {
                    c.put("chan", chan);
                    break;
                }
            }
            c.put("net", net);
            c.put("sta", sta);
        } else {
            c = new RevletContext("testNetwork.vm");
            c.put("path", path);
        }
        return c;
    }

    VelocityNetwork getNetwork(String[] parts) throws SQLException, NotFound {
        String codeOnly;
        String netCode = parts[1];
        String netyear = "";
        if (netCode.length() == 4) {
            netyear = netCode.substring(2);
            codeOnly = netCode.substring(0, 2);
        } else {
            codeOnly = netCode;
        }
        NetworkId[] nets = jdbcChannel.getNetworkTable().getByCode(codeOnly);
        NetworkId netid = null;
        if (codeOnly.startsWith("X") || codeOnly.startsWith("Y") || codeOnly.startsWith("Z")) {
            for(int i = 0; i < nets.length; i++) {
                if (NetworkIdUtil.toStringNoDates(nets[i]).equals(netCode)) {
                    netid = nets[i];
                    break;
                }
            }
        } else {
            netid = nets[0];
        }
        if (netid == null) {
            throw new NotFound(netCode);
        }
        Station[] stations = jdbcChannel.getStationTable().getAllStations(netid);
        ArrayList stationList = new ArrayList();
        for(int i = 0; i < stations.length; i++) {
            stationList.add(new VelocityStation(stations[i]));
        }
        return new VelocityNetwork(stationList);
    }
    
    VelocityStation getStation(String[] parts, VelocityNetwork net) throws SQLException, NotFound {
        Station[] stations = jdbcChannel.getStationTable().getAllStations(net.get_id());
        Station sta = null;
        for(int i = 0; i < stations.length; i++) {
            if (stations[i].get_code().equals(parts[2])) {
                sta = stations[i];
            }
        }
        return new VelocityStation(sta);
    }
    
    ArrayList getAllChannels(String[] parts, VelocityStation sta) throws NotFound, SQLException {
        Channel[] chans = jdbcChannel.getAllChannels(sta.get_id());
        ArrayList chanList = new ArrayList();
        for(int i = 0; i < chans.length; i++) {
            chanList.add(new VelocityChannel(chans[i]));
        }
        return chanList;
    }
    
    JDBCChannel jdbcChannel;
}
