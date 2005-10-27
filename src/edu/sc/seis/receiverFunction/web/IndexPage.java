package edu.sc.seis.receiverFunction.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;


public class IndexPage extends Revlet {

    public IndexPage() {
        super();
        // TODO Auto-generated constructor stub
    }

    public RevletContext getContext(HttpServletRequest req, HttpServletResponse res) throws Exception {
            RevletContext context = new RevletContext("indexPage.vm",
                                                      Start.getDefaultContext());
            Start.loadStandardQueryParams(req, context);
            return context;
    }
}
