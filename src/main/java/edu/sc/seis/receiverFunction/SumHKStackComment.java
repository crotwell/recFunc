package edu.sc.seis.receiverFunction;

import java.sql.Timestamp;

public class SumHKStackComment {

    /** hibernate*/
    public SumHKStackComment() {}
    
    public SumHKStackComment(String comment, QCUser user, Timestamp date) {
        super();
        this.comment = comment;
        this.user = user;
        this.date = date;
    }

    public int getDbid() {
        return dbid;
    }

    public void setDbid(int dbid) {
        this.dbid = dbid;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public QCUser getUser() {
        return user;
    }

    public void setUser(QCUser user) {
        this.user = user;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    protected int dbid;

    protected String comment;

    protected QCUser user;

    protected Timestamp date;
}
