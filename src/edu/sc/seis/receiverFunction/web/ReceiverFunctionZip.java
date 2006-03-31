package edu.sc.seis.receiverFunction.web;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
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
import edu.iris.dmc.seedcodec.CodecException;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.bag.DistAz;
import edu.sc.seis.fissuresUtil.bag.Rotate;
import edu.sc.seis.fissuresUtil.bag.TauPUtil;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;
import edu.sc.seis.fissuresUtil.database.network.JDBCChannel;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.sac.FissuresToSac;
import edu.sc.seis.receiverFunction.HKStack;
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
    
    protected synchronized void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, FileNotFoundException, IOException {
        int netDbId = RevUtil.getInt("netdbid", req);
        String staCode = req.getParameter("stacode");

        float gaussianWidth = RevUtil.getFloat("gaussian", req, Start.getDefaultGaussian());
        float minPercentMatch = RevUtil.getFloat("minPercentMatch", req, Start.getDefaultMinPercentMatch());
        try {
            CachedResult[] result;
            synchronized(jdbcRecFunc.getConnection()) {
                result = jdbcRecFunc.getByPercent(netDbId,
                                                  staCode,
                                                  gaussianWidth,
                                                  minPercentMatch);
            }
            String netCode = "";
            if (result.length != 0) {
                netCode = result[0].channels[0].my_site.my_station.my_network.get_code();
            }
            res.addHeader("Content-Disposition", "inline; filename="+"rf_"+netCode+"_"+staCode+".zip");
            processResults(result, req, res);
        } catch(EOFException e) {
            // client has closed the connection, so not much we can do...
            return;
        } catch(Exception e) {
            Revlet.sendToGlobalExceptionHandler(req, e);
            throw new ServletException(e);
        }
    }
    
    protected void processResults(CachedResult[] result, HttpServletRequest req, HttpServletResponse res) throws CodecException, IOException, TauModelException {
        float gaussianWidth = RevUtil.getFloat("gaussian", req, Start.getDefaultGaussian());
        res.setContentType("application/zip");
        ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(res.getOutputStream()));
        DataOutputStream dos = new DataOutputStream(zip);
        ArrayList knownEntries = new ArrayList();

        String[] pPhases = {"P"};
        TauPUtil tauPTime = TauPUtil.getTauPUtil(HKStack.modelName);
        for(int i = 0; i < result.length; i++) {
            VelocityEvent event = new VelocityEvent(new CacheEvent(result[i].event_attr,
                                                                   result[i].prefOrigin));
            VelocityStation sta = new VelocityStation(result[i].channels[2].my_site.my_station);
            // 0 for radial, 1 for transverse
            for(int rfType = 0; rfType < 2; rfType++) {
                String entryName = TOP_ZIP_DIR + "gauss_"+gaussianWidth+"/"+sta.getNetCode() + "."
                        + sta.getCode() + "/"
                        + event.getTime("yyyy_DDD_HH_mm_ss") + (rfType == 0 ? ".itr" : ".itt");
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
                // fix orientation to radial and transverse
                sac.cmpaz = (float)(rfType == 0 ? Rotate.getRadialAzimuth(sta.my_location, result[i].prefOrigin.my_location) : Rotate.getTransverseAzimuth(sta.my_location, result[i].prefOrigin.my_location));
                sac.cmpinc = 90;
                // put percent match in user0 and gaussian width in user1
                sac.user0 = (rfType == 0 ? result[i].radialMatch : result[i].transverseMatch);
                sac.kuser0 = "% match ";
                sac.user1 = result[i].config.gwidth;
                sac.kuser1 = "gwidth";
                Arrival[] arrivals = tauPTime.calcTravelTimes(result[i].channels[0].my_site.my_station,
                                                              result[i].prefOrigin,
                                                              pPhases);
                // convert radian per sec ray param into km per sec
                float kmRayParam = (float)(arrivals[0].getRayParam() / tauPTime.getTauModel()
                        .getRadiusOfEarth());
                sac.user2 = kmRayParam;
                sac.kuser2 = "rayparam";
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
            dos.writeChars("Sorry, there was no data for your request.\n");
            dos.flush();
            zip.closeEntry();
        }
        zip.close();
    }

    public void destroy() {
        try {
            Connection conn = jdbcRecFunc.getConnection();
            if (conn != null) {conn.close();}
        } catch(SQLException e) {
            // oh well
        }
        super.destroy();
    }
    
    private static final String TOP_ZIP_DIR = "Ears/";
    
    JDBCRecFunc jdbcRecFunc;
}
