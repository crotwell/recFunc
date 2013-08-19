package edu.sc.seis.receiverFunction.web;

import java.util.List;

import org.jfree.data.xy.AbstractXYZDataset;

import edu.sc.seis.receiverFunction.server.SummaryLine;

public class SummaryLineDataset extends AbstractXYZDataset {

	public SummaryLineDataset(List<SummaryLine> summary, String xAxis,
			String yAxis, String zAxis) {
		this.summary = summary;
		this.xAxis = xAxis;
		this.yAxis = yAxis;
		this.zAxis = zAxis;
	}

	public int getItemCount(int series) {
		return summary.size();
	}

	Number get(int series, int item, String key) {
	    SummaryLine sumLine = summary.get(item);
		if (HKLatLonPlot.LATITUDE.equals(key)) {
			return new Float(sumLine.getLat());
		} else if (HKLatLonPlot.LONGITUDE.equals(key)) {
			return new Float(sumLine.getLon());
		}
		if (HKLatLonPlot.THICKNESS.equals(key)) {
			return new Float(sumLine.getHkm());
		} else if (HKLatLonPlot.VPVS.equals(key)) {
			return new Float(sumLine.getVpVs());
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
	
	List<SummaryLine> summary;

	String xAxis, yAxis, zAxis;

	private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger
			.getLogger(SummaryLineDataset.class);
}