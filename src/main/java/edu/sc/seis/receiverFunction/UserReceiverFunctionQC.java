package edu.sc.seis.receiverFunction;

import java.sql.Timestamp;

import edu.sc.seis.fissuresUtil.chooser.ClockUtil;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;

public class UserReceiverFunctionQC {

    /** for hibernate */
    protected UserReceiverFunctionQC() {}
    
    public UserReceiverFunctionQC(QCUser qcUser,
                                  ReceiverFunctionResult receiverFunction,
                                  boolean keep,
                                  String reason) {
        super();
        this.qcUser = qcUser;
        this.receiverFunction = receiverFunction;
        this.keep = keep;
        this.reason = reason;
        this.insertTime = ClockUtil.now().getTimestamp();
    }

    protected long dbid;

    protected QCUser qcUser;

    protected ReceiverFunctionResult receiverFunction;

    protected boolean keep;
    
    protected String reason;

    protected Timestamp insertTime;

    public long getDbid() {
        return dbid;
    }

    public QCUser getQcUser() {
        return qcUser;
    }

    public ReceiverFunctionResult getReceiverFunction() {
        return receiverFunction;
    }

    public String getReason() {
        return reason;
    }

    public Timestamp getInsertTime() {
        return insertTime;
    }

    protected void setDbid(long dbid) {
        this.dbid = dbid;
    }

    protected void setQcUser(QCUser user) {
        this.qcUser = user;
    }

    protected void setReceiverFunction(ReceiverFunctionResult rfResult) {
        this.receiverFunction = rfResult;
    }

    protected void setReason(String reason) {
        this.reason = reason;
    }

    protected void setInsertTime(Timestamp insertTime) {
        this.insertTime = insertTime;
    }

    
    public boolean isKeep() {
        return keep;
    }

    
    protected void setKeep(boolean keep) {
        this.keep = keep;
    }
}
