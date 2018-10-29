package edu.sc.seis.receiverFunction.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.velocity.Template;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;


public abstract class XMLRevlet extends Revlet {

    protected void setContentType(HttpServletRequest request,
                                  HttpServletResponse response)
    {
        RevUtil.autoSetContentType(request, response);
    }

    public RevletContext handleNotFound(HttpServletRequest request,
                                        HttpServletResponse resp,
                                        Throwable t) {
        resp.setContentType("text/html");
        return super.handleNotFound(request, resp, t);
    }

    protected Template handleException(HttpServletRequest req, HttpServletResponse response, Throwable e) {
        response.setContentType("text/html");
        return super.handleException(req, response, e);
    }


}
