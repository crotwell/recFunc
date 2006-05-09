package edu.sc.seis.receiverFunction.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;

import edu.iris.Fissures.Time;
import edu.iris.Fissures.TimeRange;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.ChannelNotFound;
import edu.iris.Fissures.IfNetwork.NetworkAccess;
import edu.iris.Fissures.IfNetwork.NetworkAttr;
import edu.iris.Fissures.IfNetwork.NetworkDCOperations;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.IfNetwork.Station;
import edu.iris.Fissures.model.AllVTFactory;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.TimeUtils;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.network.NetworkIdUtil;
import edu.iris.Fissures.network.StationIdUtil;
import edu.sc.seis.fissuresUtil.cache.VestingNetworkDC;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.fissuresUtil.database.network.JDBCNetwork;
import edu.sc.seis.fissuresUtil.database.network.JDBCStation;
import edu.sc.seis.fissuresUtil.display.MicroSecondTimeRange;
import edu.sc.seis.fissuresUtil.namingService.FissuresNamingService;

public class CleanNetwork {

	public CleanNetwork(NetworkDCOperations netDC, Connection conn)
			throws SQLException {
		this.netDC = netDC;
		jdbcChannel = new JDBCChannel(conn);
		updateNetBegin = conn
				.prepareStatement("UPDATE network SET net_begin_id = ? "
						+ "WHERE net_id = ?");
		updateNetEnd = conn
				.prepareStatement("UPDATE network SET net_end_id = ? "
						+ "WHERE net_id = ?");
		updateStaBegin = conn
				.prepareStatement("UPDATE station SET sta_begin_id = ? "
						+ "WHERE sta_id = ?");
		updateStaEnd = conn
				.prepareStatement("UPDATE Station SET sta_end_id = ? "
						+ "WHERE sta_id = ?");
		updateSiteBegin = conn
				.prepareStatement("UPDATE site SET site_begin_id = ? "
						+ "WHERE site_id = ?");
		updateSiteEnd = conn
				.prepareStatement("UPDATE site SET site_end_id = ? "
						+ "WHERE site_id = ?");

		updateChanBegin = conn
				.prepareStatement("UPDATE channel SET chan_begin_id = ? "
						+ "WHERE sta_id = ?");
		updateChanEnd = conn
				.prepareStatement("UPDATE channel SET chan_end_id = ? "
						+ "WHERE chan_id = ?");
	}

	public void checkNetworks() throws SQLException,
			edu.sc.seis.fissuresUtil.database.NotFound {
		JDBCNetwork jdbcNet = jdbcChannel.getNetworkTable();
		NetworkAttr[] attrs = jdbcNet.getAllNetworkAttrs();
		NetworkAccess[] irisNets = netDC.a_finder().retrieve_all();
		for (int i = 0; i < attrs.length; i++) {
			checkNetwork(attrs[i], irisNets);
		}
	}

	public void checkNetwork(NetworkAttr attr, NetworkAccess[] irisNets) throws SQLException, NotFound {
		NetworkAccess irisNA = bestMatch(attr.get_id(), irisNets);
		if (irisNA == null) {
			System.out.println("Not found for: "
					+ NetworkIdUtil.toStringNoDates(attr.get_id()));
			return;
		}
		NetworkAttr irisAttr = irisNA.get_attributes();
		MicroSecondTimeRange iris = fixFuture(irisAttr.effective_time);
		MicroSecondTimeRange local = fixFuture(attr.effective_time);
		if (!iris.equals(local)) {
			System.out.println(attr.get_code()
					+ " unequal network effective times: iris=" + iris
					+ "  local=" + local);
			if (!iris.getBeginTime().equals(local.getBeginTime())) {
				// begin times different
				// if permanent net or same year, fix begin, best only finds
				// temp nets in same year
				updateTime(updateNetBegin, irisAttr.effective_time.start_time,
						jdbcChannel.getNetworkTable().getDbId(attr.get_id()));
				System.out.println("fixed net begin...");
			}
		}

	}

	void updateTime(PreparedStatement stmt, Time time, int dbid)
			throws SQLException {
		int timeDbId = jdbcChannel.getTimeTable().put(time);
		stmt.setInt(1, timeDbId);
		stmt.setInt(2, dbid);
		stmt.executeUpdate();
	}

	public static MicroSecondTimeRange fixFuture(TimeRange effective_time) {
		MicroSecondTimeRange tr = new MicroSecondTimeRange(effective_time);
		if (tr.getEndTime().after(now)) {
			tr = new MicroSecondTimeRange(tr.getBeginTime(), TimeUtils.future);
		}
		return tr;
	}

	public static NetworkAccess bestMatch(NetworkId netId, NetworkAccess[] nets) {
		for (int j = 0; j < nets.length; j++) {
			// match code and start year
			if (nets[j].get_attributes().get_code().equals(netId.network_code)
					&& (!(netId.network_code.startsWith("X")
							|| netId.network_code.startsWith("Y") || netId.network_code
							.startsWith("Z")) || nets[j].get_attributes().effective_time.start_time.date_time
							.substring(0, 4).equals(
									netId.begin_time.date_time.substring(0, 4)))) {
				return nets[j];
			}
		}
		return null;
	}

	public void checkStations() throws SQLException,
			edu.sc.seis.fissuresUtil.database.NotFound {
		JDBCStation jdbcSta = jdbcChannel.getStationTable();
		Station[] stations = jdbcSta.getAllStations();

		for (int i = 0; i < stations.length; i++) {
			checkStation(stations[i]);
		}
	}

	public boolean checkStation(Station station) throws SQLException, NotFound {
		NetworkAccess[] irisNets = netDC.a_finder().retrieve_all();
		ArrayList iris = new ArrayList();
		NetworkAccess bestNet = bestMatch(station.get_id().network_id, irisNets);
		if (bestNet == null) {
			System.out.println("Can't find net for "
					+ StationIdUtil.toStringFormatDates(station));
			return false;
		}
		MicroSecondTimeRange local = fixFuture(station.effective_time);
		MicroSecondTimeRange irisNet = fixFuture(bestNet.get_attributes().effective_time);
		if (local.getBeginTime().before(irisNet.getBeginTime())) {
			System.out.println("WARNING: station begin before net begin: "
					+ StationIdUtil.toStringFormatDates(station) + "iris="
					+ irisNet + "  local=" + local);
			return false;
		}
		Station[] irisStations = bestNet.retrieve_stations();
		for (int s = 0; s < irisStations.length; s++) {
			if (irisStations[s].get_code().equals(station.get_code())) {
				iris.add(irisStations[s]);
			}
		}

		if (iris.size() == 0) {
			System.out.println("No code match found for "
					+ StationIdUtil.toStringFormatDates(station));
			return false;
		} else {
			ArrayList overlaps = new ArrayList();
			Iterator it = iris.iterator();
			boolean found = false;
			while (it.hasNext()) {
				Station irisSta = (Station) it.next();
				MicroSecondTimeRange irisTR = fixFuture(irisSta.effective_time);

				if (irisTR.equals(local)) {
					// found and matches
					return true;
				} else if (irisTR.getBeginTime().equals(local.getBeginTime())
						&& !irisTR.getEndTime().equals(local.getEndTime())) {
					// found and end doesn't match
					found = true;
					System.out.println("Found for "
							+ StationIdUtil.toStringFormatDates(station)
							+ " but end times don't match: "
							+ irisTR.getEndTime() + " " + local.getEndTime());
					if (local.getEndTime().equals(TimeUtils.future)) {
						// looks like station ended after we received it, safe
						// to fix
						updateTime(updateStaEnd,
								irisSta.effective_time.end_time, jdbcChannel
										.getStationTable().getDBId(
												station.get_id()));
						System.out.println("fixed...");
						return true;
					}
				} else if (!irisTR.getEndTime().equals(TimeUtils.future)
						&& irisTR.getEndTime().equals(local.getEndTime())) {
					// end matches but begin doesn't
					System.out.println("Found for "
							+ StationIdUtil.toStringFormatDates(station)
							+ " but begin times don't match: "
							+ irisTR.getBeginTime() + " "
							+ local.getBeginTime());
					found = true;
				} else if (irisTR.intersects(local)) {
					overlaps.add(irisSta);
					System.out.println("Found for "
							+ StationIdUtil.toStringFormatDates(station)
							+ " possible, overlap " + irisTR + " " + local);
				}
			}
			if (!found && overlaps.size() == 0) {
				System.out.println(StationIdUtil.toString(station)
						+ " no match found");
			}
			if (overlaps.size() != 0) {
				Iterator overlapIt = overlaps.iterator();
				System.out.println("Overlaps for "
						+ StationIdUtil.toStringFormatDates(station)
						+ "   iris   " + "        local");
				while (overlapIt.hasNext()) {
					Station s = (Station) overlapIt.next();
					System.out.println("   "
							+ new MicroSecondTimeRange(s.effective_time) + "  "
							+ local);
				}
			}
		}
		return false;
	}

	public void checkChannels() throws NotFound, SQLException {
		Channel[] channels = jdbcChannel.getAllChannels();
		int numGood = 0;
		int numFixed = 0;
		int numBad = 0;
		for (int c = 0; c < channels.length; c++) {
			try {
				MicroSecondTimeRange localTR = fixFuture(channels[c].effective_time);
				Channel irisChan = netDC.a_explorer().retrieve_channel(
						channels[c].get_id());
				MicroSecondTimeRange irisTR = fixFuture(irisChan.effective_time);
				if (irisTR.equals(localTR)) {
					numGood++;
					continue;
				} else if (irisTR.getBeginTime().equals(localTR.getBeginTime())
						&& !irisTR.getEndTime().equals(localTR.getEndTime())
						&& localTR.getEndTime().equals(TimeUtils.future)) {
					System.out.println("found ended channel: "
							+ ChannelIdUtil.toStringFormatDates(channels[c]
									.get_id()) + "\n  iris=" + irisTR
							+ "\n  local=" + localTR);
					updateTime(updateChanEnd, irisChan.effective_time.end_time,
							jdbcChannel.getDBId(channels[c].get_id()));
					numFixed++;
					System.out.println("fixed...");
				} else {
					numBad++;
				}
			} catch (ChannelNotFound e) {
				System.out.println("No channel found at iris for: "
						+ ChannelIdUtil.toStringFormatDates(channels[c]
								.get_id()));
				numBad++;
			}
		}
		System.out.println("total=" + channels.length + " good=" + numGood
				+ " fixed=" + numFixed + "  bad=" + numBad);
	}

	JDBCChannel jdbcChannel;

	NetworkDCOperations netDC;

	PreparedStatement updateNetBegin, updateNetEnd;

	PreparedStatement updateStaBegin, updateStaEnd;

	PreparedStatement updateSiteBegin, updateSiteEnd;

	PreparedStatement updateChanBegin, updateChanEnd;

	static MicroSecondDate now = ClockUtil.now();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Properties props = StackSummary.loadProps(args);
			Connection conn = StackSummary.initDB(props);
			BasicConfigurator.configure();
			org.omg.CORBA_2_3.ORB orb = (org.omg.CORBA_2_3.ORB) org.omg.CORBA.ORB
					.init(new String[] {}, new Properties());
			// Registers the FISSURES classes with the ORB
			new AllVTFactory().register(orb);
			// Pick a name server to get FISSURES servers.
			FissuresNamingService namingService = new FissuresNamingService(orb);
			namingService
					.setNameServiceCorbaLoc("corbaloc:iiop:dmc.iris.washington.edu:6371/NameService");
			NetworkDCOperations netDC = new VestingNetworkDC("edu/iris/dmc",
					"IRIS_NetworkDC", namingService);
			CleanNetwork cleaner = new CleanNetwork(netDC, conn);
			cleaner.checkNetworks();
			// cleaner.checkStations();
			 cleaner.checkChannels();
		} catch (Exception e) {
			logger.error("problem in main", e);
		}
	}

	private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger
			.getLogger(CleanNetwork.class);
}
