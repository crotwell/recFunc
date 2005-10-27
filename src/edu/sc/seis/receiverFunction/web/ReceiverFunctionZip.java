package edu.sc.seis.receiverFunction.web;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.sac.FissuresToSac;
import edu.sc.seis.receiverFunction.server.JDBCRecFunc;
import edu.sc.seis.receiverFunction.server.JDBCSodConfig;
import edu.sc.seis.receiverFunction.server.RecFuncCacheImpl;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.seisFile.sac.SacTimeSeries;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.velocity.event.VelocityEvent;
import edu.sc.seis.sod.velocity.network.VelocityStation;


/**
 * @author crotwell
 * Created on Sep 2, 2005
 */
public class ReceiverFunctionZip extends HttpServlet {

    /**
     * @throws SQLException
     * @throws Exception
     * @throws ConfigurationException
     *
     */
    public ReceiverFunctionZip() throws SQLException, ConfigurationException, Exception {
        Connection conn = ConnMgr.createConnection();
        JDBCEventAccess jdbcEventAccess = new JDBCEventAccess(conn);
        JDBCChannel jdbcChannel = new JDBCChannel(conn);
        JDBCSodConfig jdbcSodConfig = new JDBCSodConfig(conn);
        jdbcRecFunc = new JDBCRecFunc(conn,
                                                  jdbcEventAccess,
                                                  jdbcChannel,
                                                  jdbcSodConfig,
                                                  RecFuncCacheImpl.getDataLoc());
    }
    
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, FileNotFoundException, IOException {
        int netDbId = RevUtil.getInt("netdbid", req);
        String staCode = req.getParameter("stacode");

        float gaussianWidth = RevUtil.getFloat("gaussian", req, Start.getDefaultGaussian());
        float minPercentMatch = RevUtil.getFloat("minPercentMatch", req, Start.getDefaultMinPercentMatch());
        try {
            CachedResult[] result = jdbcRecFunc.getByPercent(netDbId,
                                                             staCode,
                                                             gaussianWidth,
                                                             minPercentMatch);
            res.setContentType("application/zip");
            ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(res.getOutputStream()));
            DataOutputStream dos = new DataOutputStream(zip);
            ArrayList knownEntries = new ArrayList();
            for(int i = 0; i < result.length; i++) {
                VelocityEvent event = new VelocityEvent(new CacheEvent(result[i].event_attr,
                                                                       result[i].prefOrigin));
                VelocityStation sta = new VelocityStation(result[i].channels[2].my_site.my_station);
                // 0 for radial, 1 for transverse
                for(int rfType = 0; rfType < 2; rfType++) {
                    String entryName = TOP_ZIP_DIR + "gauss_"+gaussianWidth+"/"+sta.getNetCode() + "."
                            + sta.getCode() + "/"
                            + event.getTime("yyyy_DDD_HH_mm_ss") + (rfType == 0 ? ".itr" : "itt");
                    String origEntryName = entryName;
                    int j = 2;
                    while(knownEntries.contains(entryName)) {
                        entryName = origEntryName + "." + j;
                        j++;
                    }
                    knownEntries.add(entryName);
                    ZipEntry entry = new ZipEntry(entryName);
                    System.out.println("Start new ZipEntry: " + entry);
                    zip.putNextEntry(entry);
                    LocalSeismogramImpl rfSeis = (LocalSeismogramImpl) (rfType == 0 ? result[i].radial : result[i].tansverse);
                    SacTimeSeries sac = FissuresToSac.getSAC(rfSeis,
                                                             result[i].channels[2],
                                                             result[i].prefOrigin);
                    sac.writeHeader(dos);
                    sac.writeData(dos);
                    dos.flush();
                    zip.closeEntry();
                }
            }
            if(result.length == 0) {
                // add at least one file so zip doesn't complain
                ZipEntry entry = new ZipEntry(TOP_ZIP_DIR + "no_data.txt");
                zip.putNextEntry(entry);
                dos.writeChars("Sorry, there was no data for network "
                        + netDbId + " station: " + staCode + "\n");
                dos.flush();
                zip.closeEntry();
            }
            zip.close();
        } catch(Exception e) {
            Revlet.sendToGlobalExceptionHandler(req, e);
            throw new ServletException(e);
        }
    }
    
    protected void writeEntry() {
        
    }
    
    private static final String TOP_ZIP_DIR = "Ears/";
    
    JDBCRecFunc jdbcRecFunc;
}
