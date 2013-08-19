package edu.sc.seis.receiverFunction.web;

import javax.servlet.http.HttpServletRequest;

import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.receiverFunction.summaryFilter.NetworkFilter;
import edu.sc.seis.receiverFunction.summaryFilter.SummaryLineFilter;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;

public class SummaryByNetwork extends SummaryList {

    @Override
    protected SummaryLineFilter getFilter(HttpServletRequest req, RevletContext context) throws NotFound {
        VelocityNetwork net = Start.getNetwork(req);
        context.put("net", net);
        return new NetworkFilter(net.getCodeWithYear());
    }

    @Override
    public String getVelocityTemplate(HttpServletRequest req) {
        String fileType = RevUtil.getFileType(req);
        if (fileType.equals(RevUtil.MIME_CSV) || fileType.equals(RevUtil.MIME_TEXT)) {
            return "summaryByNetwork_csv.vm";
        } else {
            return "summaryByNetwork.vm";
        }
    }
}
