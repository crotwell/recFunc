package edu.sc.seis.receiverFunction.web;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.iris.Fissures.Location;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.fissuresUtil.bag.Statistics;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.crust2.Crust2;
import edu.sc.seis.receiverFunction.crust2.Crust2Profile;
import edu.sc.seis.receiverFunction.server.JDBCHKStack;
import edu.sc.seis.receiverFunction.server.JDBCRecFunc;
import edu.sc.seis.receiverFunction.server.JDBCSummaryHKStack;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;

public class Crust2GridCompare extends Revlet {

	public Crust2GridCompare() throws Exception {
		Connection conn = getConnection();
		JDBCRecFunc jdbcRecFunc = new JDBCRecFunc(conn, Start.getDataLoc());
		JDBCHKStack jdbcHKStack = new JDBCHKStack(jdbcRecFunc);
		jdbcSummaryHKStack = new JDBCSummaryHKStack(jdbcHKStack);
	}

	public RevletContext getContext(HttpServletRequest req,
			HttpServletResponse res) throws Exception {
		int minPerCell = RevUtil.getInt("minPerCell", req, 5);
		String path = req.getServletPath();
		String vmFile;
		if (path.endsWith(".txt")) {
			vmFile = "crust2GridCompareTxt.vm";
		} else if (path.endsWith(".html")) {
			vmFile = "crust2GridCompare.vm";
		} else {
			vmFile = "crust2GridCompare.vm";
		}
		RevletContext context = new RevletContext(vmFile, Start
				.getDefaultContext());
		ArrayList summaries = jdbcSummaryHKStack.getAllWithoutData();
		Iterator it = summaries.iterator();
		HashMap sumByGrid = new HashMap();
		while (it.hasNext()) {
			SumHKStack sum = (SumHKStack) it.next();
			Location loc = sum.getChannel().getSite().getStation().getLocation();
			int[] ll = Crust2.getClosestLonLat(loc.longitude, loc.latitude);
			String llStr = ll[0] + "," + ll[1];
			List gridList;
			if (!sumByGrid.containsKey(llStr)) {
				gridList = new ArrayList();
				sumByGrid.put(llStr, gridList);
			} else {
				gridList = (List) sumByGrid.get(llStr);
			}
			gridList.add(sum);
		}
		it = sumByGrid.keySet().iterator();
		ArrayList output = new ArrayList();
		context.put("gridLists", output);
		while (it.hasNext()) {
			String key = (String) it.next();
			List gridList = (List) sumByGrid.get(key);
			if (gridList.size() < minPerCell) {
				it.remove();
				continue;
			}
			Iterator listIt = gridList.iterator();
			SumHKStack sum = null;
			float[] hVals = new float[gridList.size()];
			float[] kVals = new float[gridList.size()];
			int i=0;
			while (listIt.hasNext()) {
				sum = (SumHKStack) listIt.next();
				hVals[i] = (float)sum.getSum().getMaxValueH()
						.getValue(UnitImpl.KILOMETER);
				kVals[i] = sum.getSum().getMaxValueK();
			}
			Location loc = sum.getChannel().getSite().getStation().getLocation();
			int[] lonLat = Crust2.getClosestLonLat(loc);
			output.add(new Crust2GridCompareResult(hVals, kVals, lonLat[1],
					lonLat[0], gridList, crust2.getByCode(crust2
							.getClosestCode(loc))));
		}
		return context;
	}

	protected void setContentType(HttpServletRequest req,
			HttpServletResponse response) {
		String path = req.getServletPath();
		if (path.endsWith(".txt")) {
			response.setContentType("text/plain");
		} else if (path.endsWith(".xml")) {
			response.setContentType("text/xml");
		} else if (path.endsWith(".html")) {
			response.setContentType("text/html");
		} else {
			throw new RuntimeException("Unknown URL: " + req.getRequestURI());
		}
	}

	Crust2 crust2 = new Crust2();

	JDBCSummaryHKStack jdbcSummaryHKStack;
}
