package edu.sc.seis.receiverFunction.compare;


/**
 * @author crotwell
 * Created on Mar 28, 2005
 */
public class StationResultRef {

    public StationResultRef(String name, String reference, String method) {
        this(name, reference, method, null);
    }

    public StationResultRef(String name, String reference, String method, String url) {
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
    
    public String getURL() {
        return url;
    }
    
    private String name;
    
    private String reference;

    private String method;
    
    private String url;
}
