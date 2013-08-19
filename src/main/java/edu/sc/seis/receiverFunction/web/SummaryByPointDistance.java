package edu.sc.seis.receiverFunction.web;

import javax.servlet.http.HttpServletRequest;

import edu.sc.seis.receiverFunction.summaryFilter.DistanceFilter;
import edu.sc.seis.receiverFunction.summaryFilter.SummaryLineFilter;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.RevletContext;


public class SummaryByPointDistance extends SummaryList {

    @Override
    protected SummaryLineFilter getFilter(HttpServletRequest req, RevletContext context) throws Exception {
        float lat = RevUtil.getFloat("lat", req);
        float lon = RevUtil.getFloat("lon", req);
        float delta = RevUtil.getFloat("delta", req);
        context.put("lat", new Float(lat));
        context.put("lon", new Float(lon));
        context.put("delta", new Float(delta));
        return new DistanceFilter(lat, lon, delta);
    }

    @Override
    public String getVelocityTemplate(HttpServletRequest req) {
        String fileType = RevUtil.getFileType(req);
        if (fileType.equals(RevUtil.MIME_CSV) || fileType.equals(RevUtil.MIME_TEXT)) {
            return "summaryByPointDistance_csv.vm";
        } else {
            return "summaryByPointDistance.vm";
        }
    }
}
