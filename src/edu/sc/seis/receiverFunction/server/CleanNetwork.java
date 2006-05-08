package edu.sc.seis.receiverFunction.server;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.InvalidName;
import org.omg.CosNaming.NamingContextPackage.NotFound;

import edu.iris.Fissures.Time;
import edu.iris.Fissures.TimeRange;
import edu.iris.Fissures.IfNetwork.NetworkAccess;
import edu.iris.Fissures.IfNetwork.NetworkAttr;
import edu.iris.Fissures.IfNetwork.NetworkDCOperations;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.IfNetwork.Station;
import edu.iris.Fissures.model.AllVTFactory;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.TimeUtils;
import edu.iris.Fissures.network.NetworkIdUtil;
import edu.iris.Fissures.network.StationIdUtil;
import edu.sc.seis.fissuresUtil.cache.VestingNetworkDC;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
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
	}

	public void checkNetworks() throws SQLException,
			edu.sc.seis.fissuresUtil.database.NotFound {
		JDBCNetwork jdbcNet = jdbcChannel.getNetworkTable();
		NetworkAttr[] attrs = jdbcNet.getAllNetworkAttrs();
		NetworkAccess[] irisNets = netDC.a_finder().retrieve_all();
		for (int i = 0; i < attrs.length; i++) {
			NetworkAccess irisNA = bestMatch(attrs[i].get_id(), irisNets);
			if (irisNA == null) {
				System.out.println("Not found for: "
						+ NetworkIdUtil.toStringNoDates(attrs[i].get_id()));
				continue;
			}
			NetworkAttr irisAttr = irisNA.get_attributes();
			MicroSecondTimeRange iris = fixFuture(irisAttr.effective_time);
			MicroSecondTimeRange local = fixFuture(attrs[i].effective_time);
			if (!iris.equals(local)) {
				System.out.println(attrs[i].get_code()
						+ " unequal network effective times: iris=" + iris
						+ "  local=" + local);
			}

		}
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
					&& nets[j].get_attributes().effective_time.start_time.date_time
							.substring(0, 4).equals(
									netId.begin_time.date_time.substring(0, 4))) {
				return nets[j];
			}
		}
		return null;
	}

	public void checkStations() throws SQLException,
			edu.sc.seis.fissuresUtil.database.NotFound {
		JDBCStation jdbcSta = jdbcChannel.getStationTable();
		Station[] stations = jdbcSta.getAllStations();

		NetworkAccess[] irisNets = netDC.a_finder().retrieve_all();
		for (int i = 0; i < stations.length; i++) {
			ArrayList iris = new ArrayList();
			NetworkAccess bestNet = bestMatch(stations[i].get_id().network_id,
					irisNets);
			if (bestNet == null) {
				System.out.println("Can't find net for "
						+ StationIdUtil.toStringFormatDates(stations[i]));
				continue;
			}
			Station[] irisStations = bestNet.retrieve_stations();
			for (int s = 0; s < irisStations.length; s++) {
				if (irisStations[s].get_code().equals(stations[i].get_code())) {
					iris.add(irisStations[s]);
				}
			}

			if (iris.size() == 0) {
				System.out.println("No match found for "
						+ StationIdUtil.toStringFormatDates(stations[i]));
			} else {
				MicroSecondTimeRange local = fixFuture(stations[i].effective_time);
				Iterator it = iris.iterator();
				boolean found = false;
				while (it.hasNext()) {
					Station irisSta = (Station) it.next();
					MicroSecondTimeRange irisTR = fixFuture(irisSta.effective_time);

					if (irisTR.equals(local)) {
						// found and matches
						found = true;
						break;
					} else if (irisTR.getBeginTime().equals(
							local.getBeginTime())
							&& !irisTR.getEndTime().equals(local.getEndTime())) {
						// found and end doesn't match
						found = true;
						System.out.println("Found for "
								+ StationIdUtil
										.toStringFormatDates(stations[i])
								+ " but end times don't match: "
								+ irisTR.getEndTime() + " "
								+ local.getEndTime());
					}
				}
				if (!found) {
					System.out.println(StationIdUtil.toString(stations[i])
							+ " no match found");
				}
			}
		}
	}

	JDBCChannel jdbcChannel;

	NetworkDCOperations netDC;

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
			cleaner.checkStations();
		} catch (Exception e) {
			logger.error("problem in main", e);
		}
	}

	private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger
			.getLogger(CleanNetwork.class);
}
