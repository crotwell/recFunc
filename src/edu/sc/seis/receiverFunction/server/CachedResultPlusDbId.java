package edu.sc.seis.receiverFunction.server;

import edu.sc.seis.IfReceiverFunction.CachedResult;


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
}
