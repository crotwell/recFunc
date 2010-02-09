package edu.sc.seis.receiverFunction.compare;

/**
 * @author crotwell Created on Mar 28, 2005
 */
public class StationResultRef {
    
    /** hibernate */
    protected StationResultRef() {}

    public StationResultRef(String name, String reference, String method) {
        this(name, reference, method, null);
    }

    public StationResultRef(String name,
                            String reference,
                            String method,
                            String url) {
        this.name = name;
        this.reference = reference;
        this.method = method;
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public String getName() {
        return name;
    }

    public String getReference() {
        return reference;
    }

    public String getUrl() {
        return url;
    }

    protected void setName(String name) {
        this.name = name;
    }

    protected void setReference(String reference) {
        this.reference = reference;
    }

    protected void setMethod(String method) {
        this.method = method;
    }

    protected void setUrl(String url) {
        this.url = url;
    }
    
    public int getDbid() {
        return dbid;
    }

    public void setDbid(int dbid) {
        this.dbid = dbid;
    }

    private String name;

    private String reference;

    private String method;

    private String url;
    
    private int dbid;
}
