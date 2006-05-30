package edu.sc.seis.receiverFunction.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.iris.Fissures.IfNetwork.Station;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.receiverFunction.compare.JDBCStationResult;
import edu.sc.seis.receiverFunction.compare.JDBCStationResultRef;
import edu.sc.seis.receiverFunction.server.JDBCHKStack;
import edu.sc.seis.receiverFunction.server.JDBCRecFunc;
import edu.sc.seis.receiverFunction.server.JDBCSodConfig;
import edu.sc.seis.receiverFunction.server.JDBCSummaryHKStack;
import edu.sc.seis.receiverFunction.server.StackSummary;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;
import edu.sc.seis.sod.velocity.network.VelocityStation;


public class EQRateCalc extends Revlet {
    
    public EQRateCalc() throws Exception {
        Connection conn = getConnection();
        jdbcEventAccess = new JDBCEventAccess(conn);
        jdbcChannel = new JDBCChannel(conn);
        jdbcSodConfig = new JDBCSodConfig(conn);
        jdbcRecFunc = new JDBCRecFunc(conn,
                                      jdbcEventAccess,
                                      jdbcChannel,
                                      jdbcSodConfig,
                                      Start.getDataLoc());

    }
    
    public synchronized RevletContext getContext(HttpServletRequest req,
                                                 HttpServletResponse res)
            throws Exception {
        float gaussianWidth = RevUtil.getFloat("gaussian",
                                               req,
                                               Start.getDefaultGaussian());
        float minPercentMatch = RevUtil.getFloat("minPercentMatch",
                                                 req,
                                                 Start.getDefaultMinPercentMatch());
        VelocityNetwork net = Start.getNetwork(req, jdbcChannel.getNetworkTable());
        Station[] stations = jdbcChannel.getStationTable().getAllStations(net.get_id());
        Set codes = new HashSet();
        HashMap staBegin = new HashMap();
        HashMap staEnd = new HashMap();
        HashMap stationByCode = new HashMap();
        Iterator it;
        for(int s = 0; s < stations.length; s++) {
            Station sta = stations[s];
            codes.add(sta.get_code());
            if (staBegin.containsKey(sta.get_code())) {
                MicroSecondDate begin = new MicroSecondDate(sta.effective_time.start_time);
                MicroSecondDate end = new MicroSecondDate(sta.effective_time.end_time);
                if (begin.before((MicroSecondDate)staBegin.get(sta.get_code()))) {
                   staBegin.put(sta.get_code(), begin); 
                }
                if (end.after((MicroSecondDate)staEnd.get(sta.get_code()))) {
                    staEnd.put(sta.get_code(), end);
                }
            } else {
                staBegin.put(sta.get_code(), new MicroSecondDate(sta.effective_time.start_time));
                staEnd.put(sta.get_code(), new MicroSecondDate(sta.effective_time.end_time));
            }
            stationByCode.put(sta.get_code(), new VelocityStation(sta));
        }

        String fileType = RevUtil.getFileType(req);
        String vmFile = "eqRate.vm";
        if (fileType.equals(RevUtil.MIME_CSV) || fileType.equals(RevUtil.MIME_TEXT)) {
            vmFile = "eqRateCSV.vm";
        }
        RevUtil.autoSetContentType(req, res);
        RevletContext context = new RevletContext(vmFile,
                                                  Start.getDefaultContext());
        HashMap rates = new HashMap();
        List stationList = new ArrayList();
        it = codes.iterator();
        while(it.hasNext()) {
            String staCode = (String)it.next();
            int succ = jdbcRecFunc.countSuccessfulEvents(net.getDbId(), staCode, gaussianWidth, minPercentMatch);
            int unsucc = jdbcRecFunc.countUnsuccessfulEvents(net.getDbId(), staCode, gaussianWidth, minPercentMatch);
            rates.put(staCode,
                      new EQRateStation((VelocityStation)stationByCode.get(staCode),
                                        (MicroSecondDate)staBegin.get(staCode),
                                        (MicroSecondDate)staEnd.get(staCode),
                                        succ, unsucc));
            stationList.add(stationByCode.get(staCode));
        }
        context.put("net", net);
        context.put("stationList", stationList);
        context.put("eqrates", rates);
        Revlet.loadStandardQueryParams(req, context);
        return context;
    }
    
    JDBCEventAccess jdbcEventAccess;

    JDBCChannel jdbcChannel;

    JDBCRecFunc jdbcRecFunc;

    JDBCSodConfig jdbcSodConfig;
}

