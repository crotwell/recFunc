package edu.sc.seis.receiverFunction.web;

import java.util.ArrayList;
import java.util.HashMap;

import org.jfree.data.xy.AbstractXYZDataset;

import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.sod.velocity.network.VelocityStation;

public class StationSummaryDataset extends AbstractXYZDataset {

	StationSummaryDataset(ArrayList stationList, HashMap summary, String xAxis,
			String yAxis, String zAxis) {
		this.stationList = stationList;
		this.summary = summary;
		this.xAxis = xAxis;
		this.yAxis = yAxis;
		this.zAxis = zAxis;
	}

	public int getItemCount(int series) {
		return stationList.size();
	}

	Number get(int series, int item, String key) {
		VelocityStation sta = (VelocityStation) stationList.get(item);
		if (sta == null) {
			logger.warn("getXValue station is NULL: " + series + "  " + item
					+ "  " + stationList.size());
			return new Float(0);
		}
		if (HKLatLonPlot.LATITUDE.equals(key)) {
			return new Float(sta.my_location.latitude);
		} else if (HKLatLonPlot.LONGITUDE.equals(key)) {
			return new Float(sta.my_location.longitude);
		}
		SumHKStack stack = (SumHKStack) summary.get(stationList.get(item));
		if (HKLatLonPlot.THICKNESS.equals(key)) {
			return new Float(stack.getSum().getMaxValueH().getValue(
					UnitImpl.KILOMETER));
		} else if (HKLatLonPlot.VPVS.equals(key)) {
			return new Float(stack.getSum().getMaxValueK());
		}
		// default
		logger.warn("default get: " + series + " " + item + " '" + key + "'");
		return new Integer(0);
	}

	public Number getX(int series, int item) {
		return get(series, item, xAxis);
	}

	public Number getY(int series, int item) {
		return get(series, item, yAxis);
	}

	public Number getZ(int series, int item) {
		return get(series, item, zAxis);
	}

	public int getSeriesCount() {
		return 1;
	}

	public String getSeriesName(int series) {
		return HKLatLonPlot.getLabel(yAxis);
	}

	public Comparable getSeriesKey(int arg0) {
		return null;
	}

	ArrayList stationList;

	HashMap summary;

	String xAxis, yAxis, zAxis;

	private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger
			.getLogger(StationSummaryDataset.class);
}