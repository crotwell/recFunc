package edu.sc.seis.receiverFunction.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import edu.iris.Fissures.TimeRange;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.IfNetwork.ChannelNotFound;
import edu.iris.Fissures.IfNetwork.NetworkAccess;
import edu.iris.Fissures.IfNetwork.NetworkAttr;
import edu.iris.Fissures.IfNetwork.NetworkDCOperations;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.IfNetwork.NetworkNotFound;
import edu.iris.Fissures.IfNetwork.Station;
import edu.iris.Fissures.model.AllVTFactory;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.TimeUtils;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.network.ChannelImpl;
import edu.iris.Fissures.network.NetworkAttrImpl;
import edu.iris.Fissures.network.NetworkIdUtil;
import edu.iris.Fissures.network.StationIdUtil;
import edu.sc.seis.fissuresUtil.cache.ProxyNetworkDC;
import edu.sc.seis.fissuresUtil.cache.VestingNetworkDC;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.display.MicroSecondTimeRange;
import edu.sc.seis.fissuresUtil.hibernate.HibernateUtil;
import edu.sc.seis.fissuresUtil.hibernate.NetworkDB;
import edu.sc.seis.fissuresUtil.namingService.FissuresNamingService;
import edu.sc.seis.fissuresUtil.simple.Initializer;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.rev.admin.Args;
import edu.sc.seis.rev.hibernate.RevDB;
import edu.sc.seis.sod.Start;
import edu.sc.seis.sod.hibernate.SodDB;

public class CleanNetwork {

    public CleanNetwork(ProxyNetworkDC netDC) {
        this.netDC = netDC;
    }

    public void checkNetworks() throws
            edu.sc.seis.fissuresUtil.database.NotFound {
        List<NetworkAttrImpl> attrs = NetworkDB.getSingleton().getAllNetworks();
        NetworkAccess[] irisNets = netDC.a_finder().retrieve_all();
        for(NetworkAttrImpl net : attrs) {
            checkNetwork(net, irisNets);
        }
    }

    public void checkNetwork(NetworkAttrImpl attr, NetworkAccess[] irisNets)
            throws NotFound {
        NetworkAccess irisNA = bestMatch(attr.get_id(), irisNets);
        if(irisNA == null) {
            System.out.println("Not found for: "
                    + NetworkIdUtil.toStringNoDates(attr.get_id()));
            return;
        }
        NetworkAttr irisAttr = irisNA.get_attributes();
        MicroSecondTimeRange iris = fixFuture(irisAttr.getEffectiveTime());
        MicroSecondTimeRange local = fixFuture(attr.getEffectiveTime());
        if(!iris.equals(local)) {
            System.out.println(attr.get_code()
                    + " unequal network effective times: iris=" + iris
                    + "  local=" + local);
            if(!iris.getBeginTime().equals(local.getBeginTime())) {
                // begin times different
                // if permanent net or same year, fix begin, best only finds
                // temp nets in same year
                attr.updateBeginTime(irisAttr.getEffectiveTime().start_time);
                NetworkDB.getSession().saveOrUpdate(attr);
                NetworkDB.commit();
                System.out.println("fixed net begin...");
            }
        }
    }

    public static MicroSecondTimeRange fixFuture(TimeRange effective_time) {
        MicroSecondTimeRange tr = new MicroSecondTimeRange(effective_time);
        if(tr.getEndTime().after(now)) {
            tr = new MicroSecondTimeRange(tr.getBeginTime(), TimeUtils.future);
        }
        return tr;
    }

    public static NetworkAccess bestMatch(NetworkId netId, NetworkAccess[] nets) {
        for(int j = 0; j < nets.length; j++) {
            // match code and start year
            if(nets[j].get_attributes().get_code().equals(netId.network_code)
                    && (!(netId.network_code.startsWith("X")
                            || netId.network_code.startsWith("Y") || netId.network_code.startsWith("Z")) || nets[j].get_attributes().getEffectiveTime().start_time.date_time.substring(0,
                                                                                                                                                                                   4)
                            .equals(netId.begin_time.date_time.substring(0, 4)))) {
                return nets[j];
            }
        }
        return null;
    }

    public void checkStations() throws NetworkNotFound,
            edu.sc.seis.fissuresUtil.database.NotFound {
        Station[] stations = NetworkDB.getSingleton().getAllStations();
        for(int i = 0; i < stations.length; i++) {
            checkStation(stations[i]);
        }
    }

    public boolean checkStation(Station station) throws NotFound, NetworkNotFound {
        NetworkAccess[] irisNets = netDC.a_finder().retrieve_by_code(station.get_id().network_id.network_code);
        ArrayList iris = new ArrayList();
        NetworkAccess bestNet = bestMatch(station.get_id().network_id, irisNets);
        if(bestNet == null) {
            System.out.println("Can't find net for "
                    + StationIdUtil.toStringFormatDates(station));
            return false;
        }
        MicroSecondTimeRange local = fixFuture(station.getEffectiveTime());
        MicroSecondTimeRange irisNet = fixFuture(bestNet.get_attributes().getEffectiveTime());
        if(local.getBeginTime().before(irisNet.getBeginTime())) {
            System.out.println("WARNING: station begin before net begin: "
                    + StationIdUtil.toStringFormatDates(station) + "iris="
                    + irisNet + "  local=" + local);
            return false;
        }
        Station[] irisStations = bestNet.retrieve_stations();
        for(int s = 0; s < irisStations.length; s++) {
            if(irisStations[s].get_code().equals(station.get_code())) {
                iris.add(irisStations[s]);
            }
        }
        if(iris.size() == 0) {
            System.out.println("No code match found for "
                    + StationIdUtil.toStringFormatDates(station));
            return false;
        } else {
            ArrayList overlaps = new ArrayList();
            Iterator it = iris.iterator();
            boolean found = false;
            while(it.hasNext()) {
                Station irisSta = (Station)it.next();
                MicroSecondTimeRange irisTR = fixFuture(irisSta.getEffectiveTime());
                if(irisTR.equals(local)) {
                    // found and matches
                    return true;
                } else if(irisTR.getBeginTime().equals(local.getBeginTime())
                        && !irisTR.getEndTime().equals(local.getEndTime())) {
                    // found and end doesn't match
                    found = true;
                    System.out.println("Found for "
                            + StationIdUtil.toStringFormatDates(station)
                            + " but end times don't match: "
                            + irisTR.getEndTime() + " " + local.getEndTime());
                    if(local.getEndTime().equals(TimeUtils.future)) {
                        // looks like station ended after we received it, safe
                        // to fix
                        station.setEndTime(irisSta.getEffectiveTime().end_time);
                        NetworkDB.getSession().saveOrUpdate(station);
                        NetworkDB.commit();
                        NetworkDB.commit();
                        System.out.println("fixed...");
                        return true;
                    }
                } else if(!irisTR.getEndTime().equals(TimeUtils.future)
                        && irisTR.getEndTime().equals(local.getEndTime())) {
                    // end matches but begin doesn't
                    System.out.println("Found for "
                            + StationIdUtil.toStringFormatDates(station)
                            + " but begin times don't match: "
                            + irisTR.getBeginTime() + " "
                            + local.getBeginTime());
                    found = true;
                } else if(irisTR.intersects(local)) {
                    overlaps.add(irisSta);
                    System.out.println("Found for "
                            + StationIdUtil.toStringFormatDates(station)
                            + " possible, overlap " + irisTR + " " + local);
                }
            }
            if(!found && overlaps.size() == 0) {
                System.out.println(StationIdUtil.toString(station)
                        + " no match found");
            }
            if(overlaps.size() != 0) {
                Iterator overlapIt = overlaps.iterator();
                System.out.println("Overlaps for "
                        + StationIdUtil.toStringFormatDates(station)
                        + "   iris   " + "        local");
                while(overlapIt.hasNext()) {
                    Station s = (Station)overlapIt.next();
                    System.out.println("   "
                            + new MicroSecondTimeRange(s.getEffectiveTime()) + "  "
                            + local);
                }
            }
        }
        return false;
    }

    public void checkChannels() throws NotFound {
        List<ChannelImpl> channels = NetworkDB.getSingleton().getAllChannels();
        int numGood = 0;
        int numFixed = 0;
        int numBad = 0;
        NetworkAccess[] nets = netDC.a_finder().retrieve_all();
        for(Channel chan : channels) {
            try {
                MicroSecondTimeRange localTR = fixFuture(chan.getEffectiveTime());
                NetworkAccess net = null;
                for(int i = 0; i < nets.length; i++) {
                    if(NetworkIdUtil.areEqual(nets[i].get_attributes().get_id(),
                                              chan.get_id().network_id)) {
                        net = nets[i];
                    }
                }
                if(net == null) {
                    System.out.println("Can't find network for channel: "
                            + ChannelIdUtil.toString(chan.get_id()));
                }
                ChannelId chanId = chan.get_id();
                Channel[] matchChan = net.retrieve_channels_by_code(chanId.station_code,
                                                                    chanId.site_code,
                                                                    chanId.channel_code);
                ArrayList overlaps = new ArrayList();
                ArrayList overlapChans = new ArrayList();
                boolean found = false;
                for(int i = 0; i < matchChan.length; i++) {
                    Channel irisChan = matchChan[i];
                    MicroSecondTimeRange irisTR = fixFuture(irisChan.getEffectiveTime());
                    if(irisTR.equals(localTR)) {
                        numGood++;
                        found = true;
                        break;
                    } else if(irisTR.getBeginTime()
                            .equals(localTR.getBeginTime())
                            && !irisTR.getEndTime()
                                    .equals(localTR.getEndTime())
                            && localTR.getEndTime().equals(TimeUtils.future)) {
                        System.out.println("found ended channel: "
                                + ChannelIdUtil.toStringFormatDates(chan.get_id())
                                + "\n  iris=" + irisTR + "\n  local=" + localTR);
                        chan.setEndTime(irisChan.getEffectiveTime().end_time);
                        NetworkDB.getSession().saveOrUpdate(chan);
                        NetworkDB.commit();
                        numFixed++;
                        found = true;
                        System.out.println("fixed...");
                        break;
                    } else {
                        if(irisTR.intersects(localTR)) {
                            overlaps.add(irisTR);
                            overlapChans.add(irisChan);
                        }
                    }
                }
                if(!found) {
                    if(overlaps.size() != 0) {
                        if(overlaps.size() == 1) {
                            // go ahead and fix:
                            Channel irisChan = (Channel)overlapChans.get(0);
                            MicroSecondTimeRange irisTR = fixFuture(irisChan.getEffectiveTime());
                            chan.setBeginTime(irisChan.getEffectiveTime().start_time);
                            chan.setEndTime(irisChan.getEffectiveTime().end_time);
                            NetworkDB.getSession().saveOrUpdate(chan);
                            NetworkDB.commit();
                            numFixed++;
                            System.out.println("Single overlap, fixed "
                                               + irisTR
                                               + "  "
                                               + ChannelIdUtil.toStringNoDates(chan.get_id()));
                            found = true;
                        } else {
                            Iterator it = overlaps.iterator();
                            System.out.println("Overlaps "
                                    + localTR
                                    + "  "
                                    + ChannelIdUtil.toStringNoDates(chan.get_id()));
                            while(it.hasNext()) {
                                System.out.println("         " + it.next());
                            }
                        }
                    } else {
                        System.out.println("No overlaps: "
                                + ChannelIdUtil.toStringNoDates(chan.get_id())
                                + ": " + localTR);
                    }
                    if (!found){numBad++;}
                }
            } catch(ChannelNotFound e) {
                System.out.println("No channel found at iris for: "
                        + ChannelIdUtil.toStringFormatDates(chan.get_id()));
                numBad++;
            }
        }
        System.out.println("total=" + channels.size() + " good=" + numGood
                + " fixed=" + numFixed + "  bad=" + numBad);
    }

    NetworkDCOperations netDC;

    static MicroSecondDate now = ClockUtil.now();

    /**
     * @param args
     */
    public static void main(String[] inArgs) {
        Properties props = System.getProperties();
        try {
            Args args = new Args(inArgs);
            // get some defaults
            //Initializer.loadProps((Start.class).getClassLoader()
            //        .getResourceAsStream(DEFAULT_PROPS), props);
            if(args.hasProps()) {
                try {
                    Initializer.loadProps(args.getProps(), props);
                } catch(IOException io) {
                    System.err.println("Unable to load props file: "
                            + io.getMessage());
                    System.err.println("Quitting until the error is corrected");
                    System.exit(1);
                }
            }
            PropertyConfigurator.configure(props);
            ConnMgr.installDbProperties(props, args.getInitialArgs());
            synchronized(HibernateUtil.class) {
                HibernateUtil.setUpFromConnMgr(props, HibernateUtil.DEFAULT_EHCACHE_CONFIG);
                SodDB.configHibernate(HibernateUtil.getConfiguration());
                RevDB.configHibernate(HibernateUtil.getConfiguration());
                RecFuncDB.configHibernate(HibernateUtil.getConfiguration());
            }
            org.omg.CORBA_2_3.ORB orb = (org.omg.CORBA_2_3.ORB)org.omg.CORBA.ORB.init(new String[] {},
                                                                                      new Properties());
            // Registers the FISSURES classes with the ORB
            new AllVTFactory().register(orb);
            // Pick a name server to get FISSURES servers.
            FissuresNamingService namingService = new FissuresNamingService(orb);
            namingService.setNameServiceCorbaLoc("corbaloc:iiop:dmc.iris.washington.edu:6371/NameService");
            ProxyNetworkDC netDC = new VestingNetworkDC("edu/iris/dmc",
                                                             "IRIS_NetworkDC",
                                                             namingService);
            CleanNetwork cleaner = new CleanNetwork(netDC);
            // cleaner.checkNetworks();
            // cleaner.checkStations();
            cleaner.checkChannels();
        } catch(Exception e) {
            logger.error("problem in main", e);
        }
    }

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CleanNetwork.class);
}
