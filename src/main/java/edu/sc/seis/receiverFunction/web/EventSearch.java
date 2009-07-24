package edu.sc.seis.receiverFunction.web;

import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;


public class EventSearch extends Revlet {

    public EventSearch() throws SQLException {
        super();
        // TODO Auto-generated constructor stub
    }

    public RevletContext getContext(HttpServletRequest req, HttpServletResponse res) throws Exception {
        RevletContext context = new RevletContext("eventSearchForm.vm", Start.getDefaultContext());
        return context;
    }
    
    
}
