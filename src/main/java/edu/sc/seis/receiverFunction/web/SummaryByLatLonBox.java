package edu.sc.seis.receiverFunction.web;

import javax.servlet.http.HttpServletRequest;

import edu.sc.seis.receiverFunction.summaryFilter.LatLonBoxFilter;
import edu.sc.seis.receiverFunction.summaryFilter.SummaryLineFilter;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.RevletContext;


public class SummaryByLatLonBox extends SummaryList {

    @Override
    protected SummaryLineFilter getFilter(HttpServletRequest req, RevletContext context) throws Exception {
        float minLat = RevUtil.getFloat("minLat", req);
        float minLon = RevUtil.getFloat("minLon", req);
        float maxLat = RevUtil.getFloat("maxLat", req);
        float maxLon = RevUtil.getFloat("maxLon", req);
        context.put("minLat", new Float(minLat));
        context.put("minLon", new Float(minLon));
        context.put("maxLat", new Float(maxLat));
        context.put("maxLon", new Float(maxLon));
        return new LatLonBoxFilter(minLat, maxLat, minLon, maxLon);
    }

    @Override
    public String getVelocityTemplate(HttpServletRequest req) {
        String fileType = RevUtil.getFileType(req);
        if (fileType.equals(RevUtil.MIME_CSV) || fileType.equals(RevUtil.MIME_TEXT)) {
            return "summaryByLatLonBox_csv.vm";
        } else {
            return "summaryByLatLonBox.vm";
        }
    }
}
