package edu.sc.seis.receiverFunction;

import java.sql.Timestamp;

import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;

public class UserReceiverFunctionQC {

    protected long dbid;

    protected QCUser user;

    protected ReceiverFunctionResult rfResult;

    protected String reason;

    protected Timestamp insertTime;

    public long getDbid() {
        return dbid;
    }

    public QCUser getUser() {
        return user;
    }

    public ReceiverFunctionResult getRfResult() {
        return rfResult;
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

    protected void setUser(QCUser user) {
        this.user = user;
    }

    protected void setRfResult(ReceiverFunctionResult rfResult) {
        this.rfResult = rfResult;
    }

    protected void setReason(String reason) {
        this.reason = reason;
    }

    protected void setInsertTime(Timestamp insertTime) {
        this.insertTime = insertTime;
    }
}
