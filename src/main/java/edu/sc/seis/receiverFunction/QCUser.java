package edu.sc.seis.receiverFunction;

public class QCUser {
    /** for hibernate */
    protected QCUser() {}

    public QCUser(String name, String passwordHash, String email) {
        super();
        this.name = name;
        this.passwordHash = passwordHash;
        this.email = email;
    }
    
    protected long dbid;
    
    protected String name;

    protected String passwordHash;
    
    protected String email;

    
    public String getEmail() {
        return email;
    }
    
    protected void setEmail(String email) {
        this.email = email;
    }

    protected void setName(String name) {
        this.name = name;
    }

    protected void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getName() {
        return name;
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
