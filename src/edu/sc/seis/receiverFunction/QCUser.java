package edu.sc.seis.receiverFunction;

public class QCUser {

    /** for hibernate */
    protected QCUser() {}

    public QCUser(String username, String passwordHash, String email) {
        super();
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
    }

    protected long dbid;

    protected String username;

    protected String passwordHash;

    protected String email;

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

    public String getEmail() {
        return email;
    }

    protected void setEmail(String email) {
        this.email = email;
    }
}
