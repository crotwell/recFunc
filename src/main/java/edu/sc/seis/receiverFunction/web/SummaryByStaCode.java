package edu.sc.seis.receiverFunction.web;

import javax.servlet.http.HttpServletRequest;

import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.receiverFunction.summaryFilter.StationCodeFilter;
import edu.sc.seis.receiverFunction.summaryFilter.SummaryLineFilter;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.RevletContext;


public class SummaryByStaCode extends SummaryList {

    @Override
    protected SummaryLineFilter getFilter(HttpServletRequest req, RevletContext context) throws NotFound {
        String staCode = RevUtil.get("stacode", req);
        context.put("stacode", staCode);
        return new StationCodeFilter(staCode);
    }

    @Override
    public String getVelocityTemplate(HttpServletRequest req) {
        String fileType = RevUtil.getFileType(req);
        if (fileType.equals(RevUtil.MIME_CSV) || fileType.equals(RevUtil.MIME_TEXT)) {
            return "summaryByStaCode_csv.vm";
        } else {
            return "summaryByStaCode.vm";
        }
    }
}
