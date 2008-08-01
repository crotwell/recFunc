package edu.sc.seis.receiverFunction.web;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.hibernate.NetworkDB;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.velocity.network.VelocityStation;

public class Overview extends StationList {

	public Overview() throws SQLException, ConfigurationException, Exception {
		super();
		// TODO Auto-generated constructor stub
	}

	public ArrayList getStations(HttpServletRequest req, RevletContext context)
			throws SQLException, NotFound {
		checkDataLoaded(req);
		int minEQ = RevUtil.getInt("minEQ", req, 2);
		HashMap out = new HashMap();
		for (Iterator iter = data.keySet().iterator(); iter.hasNext();) {
			VelocityStation key = (VelocityStation) iter.next();
			SumHKStack stack = (SumHKStack) data.get(key);
			if (stack.getNumEQ() >= minEQ) {
				out.put(key, stack);
			}
		}
		return new ArrayList(out.keySet());
	}

	public HashMap getSummaries(HttpServletRequest req, ArrayList stationList,
			RevletContext context) throws SQLException, IOException {
		checkDataLoaded(req);
		HashMap out = new HashMap();
		for (Iterator iter = stationList.iterator(); iter.hasNext();) {
			Object key = iter.next();
			out.put(key, data.get(key));
		}
		return out;
	}

	public String getVelocityTemplate(HttpServletRequest req) {
		if (req.getServletPath().endsWith(".txt")) {
			return "overview_txt.vm";
		} else if (req.getServletPath().endsWith(".html")) {
			return "overview_html.vm";
		} else {
			throw new RuntimeException("Unknown URL: " + req.getRequestURI());
		}
	}

	synchronized void checkDataLoaded(HttpServletRequest req) {
		if (data == null
				|| loadtime.subtract(ClockUtil.now()).greaterThan(CACHE_TIME)) {
	        float gaussianWidth = RevUtil.getFloat("gaussian",
	                                               req,
	                                               Start.getDefaultGaussian());
			data = new HashMap<VelocityStation, SumHKStack>();
			loadtime = ClockUtil.now();
			List<SumHKStack> summaryList = RecFuncDB.getSingleton().getAllSumStack(gaussianWidth);
            for (Iterator<SumHKStack> iter = summaryList.iterator(); iter.hasNext();) {
            	SumHKStack stack = (SumHKStack) iter.next();
            	data.put(new VelocityStation(NetworkDB.getSingleton().getStationForNet(stack.getNet(), stack.getStationCode()).get(0))
            			, stack);
            }
		}
	}

	HashMap<VelocityStation, SumHKStack> data = null;

	MicroSecondDate loadtime;

	public static TimeInterval CACHE_TIME = new TimeInterval(24, UnitImpl.HOUR);
}