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
	}

	public ArrayList getStations(HttpServletRequest req, RevletContext context)
			throws SQLException, NotFound {
        Float gaussianWidth = RevUtil.getFloat("gaussian",
                                               req,
                                               Start.getDefaultGaussian());
		int minEQ = RevUtil.getInt("minEQ", req, 2);
		HashMap out = new HashMap();
		HashMap dataMap = SummaryCache.getSingleton().getSummaries(gaussianWidth);
		for (Iterator iter = dataMap.keySet().iterator(); iter.hasNext();) {
			VelocityStation key = (VelocityStation) iter.next();
			SumHKStack stack = (SumHKStack) dataMap.get(key);
			if (stack.getNumEQ() >= minEQ) {
				out.put(key, stack);
			}
		}
		return new ArrayList(out.keySet());
	}

    public HashMap<VelocityStation, SumHKStack> getSummaries(ArrayList stationList,
                                RevletContext context,
                                HttpServletRequest req) throws SQLException,
            IOException, NotFound {
        Float gaussianWidth = RevUtil.getFloat("gaussian",
                                               req,
                                               Start.getDefaultGaussian());
        HashMap<VelocityStation, SumHKStack> dataMap = SummaryCache.getSingleton().getSummaries(gaussianWidth);
		HashMap out = new HashMap();
		for (Iterator iter = stationList.iterator(); iter.hasNext();) {
			Object key = iter.next();
			out.put(key, dataMap.get(key));
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

}