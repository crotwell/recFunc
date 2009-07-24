package edu.sc.seis.receiverFunction.rest;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.NetworkAttr;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.IfNetwork.Station;
import edu.iris.Fissures.network.ChannelImpl;
import edu.iris.Fissures.network.NetworkAttrImpl;
import edu.iris.Fissures.network.NetworkIdUtil;
import edu.iris.Fissures.network.StationImpl;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.hibernate.NetworkDB;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.velocity.network.VelocityChannel;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;
import edu.sc.seis.sod.velocity.network.VelocityStation;

public class Network extends Revlet {

    public Network() {
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
            List<NetworkAttrImpl> nets = NetworkDB.getSingleton().getAllNetworks();
            ArrayList<VelocityNetwork> list = new ArrayList<VelocityNetwork>();
            for(NetworkAttrImpl n : nets) {
                list.add(new VelocityNetwork(n));
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
                if (chan.getSite().get_code().equals(parts[3]) && chan.get_code().equals(parts[4])) {
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
        List<NetworkAttrImpl> nets = NetworkDB.getSingleton().getNetworkByCode(codeOnly);
        NetworkAttrImpl netid = null;
        if (codeOnly.startsWith("X") || codeOnly.startsWith("Y") || codeOnly.startsWith("Z")) {
            for(NetworkAttrImpl networkAttrImpl : nets) {
                if (NetworkIdUtil.toStringNoDates(networkAttrImpl).equals(netCode)) {
                    netid = networkAttrImpl;
                    break;
                }
            }
        } else {
            netid = nets.get(0);
        }
        if (netid == null) {
            throw new NotFound(netCode);
        }
        List<StationImpl> stations = NetworkDB.getSingleton().getStationForNet(netid);
        ArrayList<VelocityStation> stationList = new ArrayList<VelocityStation>();
        for(StationImpl stationImpl : stations) {
            stationList.add(new VelocityStation(stationImpl));
        }
        return new VelocityNetwork(stationList);
    }
    
    VelocityStation getStation(String[] parts, VelocityNetwork net) throws SQLException, NotFound {
        List<StationImpl> stations = NetworkDB.getSingleton().getStationForNet(net.getWrapped());
        StationImpl sta = null;
        for(StationImpl stationImpl : stations) {
            if (stationImpl.get_code().equals(parts[2])) {
                sta = stationImpl;
            }
        }
        return new VelocityStation(sta);
    }
    
    ArrayList<VelocityChannel> getAllChannels(String[] parts, VelocityStation sta) throws NotFound, SQLException {
        ArrayList<VelocityChannel> chanList = new ArrayList<VelocityChannel>();
        List<ChannelImpl> chans = NetworkDB.getSingleton().getChannelsForStation(sta.getWrapped());
        for(ChannelImpl channelImpl : chans) {
            chanList.add(new VelocityChannel(channelImpl));
        }
        return chanList;
    }
}
