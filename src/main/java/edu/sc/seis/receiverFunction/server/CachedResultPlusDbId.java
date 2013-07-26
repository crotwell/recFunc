package edu.sc.seis.receiverFunction.server;

import edu.iris.Fissures.event.EventAttrImpl;
import edu.iris.Fissures.event.OriginImpl;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.chooser.ThreadSafeDecimalFormat;


public class CachedResultPlusDbId {

    public CachedResultPlusDbId(CachedResult cachedResult, int dbid) {
        super();
        this.cachedResult = cachedResult;
        this.dbid = dbid;
    }

    private CachedResult cachedResult;

    private int dbid;
    
    public CachedResult getCachedResult() {
        return cachedResult;
    }
    
    public int getDbId() {
        return dbid;
    }
    public CacheEvent getEvent() {
        return new CacheEvent((EventAttrImpl)getCachedResult().event_attr, (OriginImpl)getCachedResult().prefOrigin);
    }
    
    public String formatRadialMatch() {
        return df.format(getCachedResult().radialMatch);
    }
    
    private ThreadSafeDecimalFormat df = new ThreadSafeDecimalFormat("0.0");
}
