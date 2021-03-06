package edu.sc.seis.receiverFunction.hibernate;

import java.sql.Timestamp;

import edu.iris.Fissures.network.NetworkAttrImpl;
import edu.iris.Fissures.network.NetworkIdUtil;
import edu.sc.seis.fissuresUtil.chooser.ClockUtil;

public class RFInsertion {

    /** for hibernate */
    protected RFInsertion() {}

    public RFInsertion(NetworkAttrImpl net,
                       String staCode,
                       float gaussianWidth) {
        this(net, staCode, gaussianWidth, ClockUtil.now().getTimestamp());
    }
    
    public RFInsertion(NetworkAttrImpl net,
                       String staCode,
                       float gaussianWidth,
                       Timestamp insertTime) {
        super();
        this.net = net;
        this.staCode = staCode;
        this.gaussianWidth = gaussianWidth;
        this.insertTime = insertTime;
    }

    protected void setDbid(int dbid) {
        this.dbid = dbid;
    }

    protected void setNet(NetworkAttrImpl net) {
        this.net = net;
    }

    protected void setStaCode(String staCode) {
        this.staCode = staCode;
    }

    public void setInsertTime(Timestamp insertTime) {
        this.insertTime = insertTime;
    }

    public int getDbid() {
        return dbid;
    }

    public NetworkAttrImpl getNet() {
        return net;
    }

    public String getStaCode() {
        return staCode;
    }
    
    public String netStaCode() {
        return NetworkIdUtil.toStringNoDates(getNet())+"."+getStaCode();
    }

    public Timestamp getInsertTime() {
        return insertTime;
    }

    public float getGaussianWidth() {
        return gaussianWidth;
    }

    protected void setGaussianWidth(float gaussianWidth) {
        this.gaussianWidth = gaussianWidth;
    }

    private int dbid;

    private NetworkAttrImpl net;

    private String staCode;

    private float gaussianWidth;

    private Timestamp insertTime;
}
