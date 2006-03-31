package edu.sc.seis.receiverFunction.server;

import java.text.DecimalFormat;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;


public class CachedResultPlusDbId {
    
    private CachedResult cachedResult;
    private int dbid;
    
    public CachedResult getCachedResult() {
        return cachedResult;
    }
    
    public int getDbId() {
        return dbid;
    }

    public CachedResultPlusDbId(CachedResult cachedResult, int dbid) {
        super();
        // TODO Auto-generated constructor stub
        this.cachedResult = cachedResult;
        this.dbid = dbid;
    }
    
    public CacheEvent getEvent() {
        return new CacheEvent(getCachedResult().event_attr, getCachedResult().prefOrigin);
    }
    
    public String formatRadialMatch() {
        return df.format(getCachedResult().radialMatch);
    }
    
    private DecimalFormat df = new DecimalFormat("0.0");
}
