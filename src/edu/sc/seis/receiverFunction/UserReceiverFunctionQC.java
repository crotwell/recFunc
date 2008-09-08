package edu.sc.seis.receiverFunction;

import java.sql.Timestamp;

import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;

public class UserReceiverFunctionQC {

    protected long dbid;
    
    protected QCUser user;

    protected ReceiverFunctionResult rfResult;

    protected String reason;

    protected Timestamp insertTime;
}
