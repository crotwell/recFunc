package edu.sc.seis.receiverFunction;

public class QCUser {

    protected long dbid;
    
    protected String username;

    protected String passwordHash;

    protected void setUsername(String username) {
        this.username = username;
    }

    protected void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public long getDbid() {
        return dbid;
    }

    protected void setDbid(long dbid) {
        this.dbid = dbid;
    }
}
