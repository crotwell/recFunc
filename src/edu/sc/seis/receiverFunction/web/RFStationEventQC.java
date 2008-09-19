package edu.sc.seis.receiverFunction.web;

import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.receiverFunction.QCUser;
import edu.sc.seis.receiverFunction.UserReceiverFunctionQC;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;
import edu.sc.seis.receiverFunction.server.SyntheticFactory;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.ConfigurationException;

public class RFStationEventQC extends RFStationEvent {

    public RFStationEventQC() throws SQLException, ConfigurationException,
            Exception {}

    @Override
    public synchronized RevletContext getContext(HttpServletRequest req,
                                                 HttpServletResponse res)
            throws Exception {
        ReceiverFunctionResult result;
        if(!(RevUtil.exists("rf", req) && RevUtil.exists("keep", req) && RevUtil.exists("reason",
                                                                                        req))) {
            throw new Exception("Missing params");
        }
        if(!RevUtil.get("rf", req).equals("synth")) {
            int rfid = RevUtil.getInt("rf", req);
            result = RecFuncDB.getSingleton().getReceiverFunctionResult(rfid);
            if(result == null) {
                handleNotFound(req, res, new NotFound());
            }
            QCUser qcUser = RecFuncDB.getSingleton().getQCUser(RevUtil.get("userHash", req));
            UserReceiverFunctionQC qc = new UserReceiverFunctionQC(qcUser,
                                                                   result,
                                                                   RevUtil.getBoolean("keep",
                                                                                      req,
                                                                                      false),
                                                                   RevUtil.get("reason",
                                                                               req));
            RecFuncDB.getSingleton().put(qc);
            RecFuncDB.commit();
        }
        return super.getContext(req, res);
    }
}
