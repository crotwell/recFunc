package edu.sc.seis.receiverFunction.compare;


/**
 * @author crotwell
 * Created on Mar 28, 2005
 */
public class StationResultRef {

    /**
     *
     */
    public StationResultRef(String name, String reference, String method) {
        this.name = name;
        this.reference = reference;
        this.method = method;
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
    
    private String name;
    
    private String reference;

    private String method;
}
