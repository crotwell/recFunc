package edu.sc.seis.receiverFunction.web;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.iris.Fissures.IfEvent.NoPreferredOrigin;
import edu.iris.Fissures.network.StationImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.iris.dmc.seedcodec.CodecException;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.bag.Rotate;
import edu.sc.seis.fissuresUtil.bag.TauPUtil;
import edu.sc.seis.fissuresUtil.sac.FissuresToSac;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.receiverFunction.hibernate.ReceiverFunctionResult;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.seisFile.sac.SacTimeSeries;
import edu.sc.seis.sod.velocity.event.VelocityEvent;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;
import edu.sc.seis.sod.velocity.network.VelocityStation;

/**
 * @author crotwell Created on Sep 2, 2005
 */
public class ReceiverFunctionZip extends HttpServlet {

    protected synchronized void doGet(HttpServletRequest req,
                                      HttpServletResponse res)
            throws ServletException, FileNotFoundException, IOException {
        try {
            VelocityNetwork net = Start.getNetwork(req);
            String staCode = req.getParameter("stacode");
            float gaussianWidth = RevUtil.getFloat("gaussian",
                                                   req,
                                                   Start.getDefaultGaussian());
            float minPercentMatch = RevUtil.getFloat("minPercentMatch",
                                                     req,
                                                     Start.getDefaultMinPercentMatch());
            List<ReceiverFunctionResult> result = RecFuncDB.getSingleton()
                    .getSuccessful(net.getWrapped(),
                                   staCode,
                                   gaussianWidth,
                                   minPercentMatch);
            String netCode = "";
            if(result.size() != 0) {
                netCode = result.get(0)
                        .getChannelGroup()
                        .getChannel1()
                        .getSite()
                        .getStation()
                        .getNetworkAttr()
                        .get_code();
            }
            res.addHeader("Content-Disposition", "inline; filename=" + "ears_"
                    + netCode + "_" + staCode + ".zip");
            processResults(result, req, res);
        } catch(EOFException e) {
            // client has closed the connection, so not much we can do...
            return;
        } catch(Exception e) {
            Revlet.sendToGlobalExceptionHandler(req, e);
            throw new ServletException(e);
        }
    }

    protected void processResults(List<ReceiverFunctionResult> results,
                                  HttpServletRequest req,
                                  HttpServletResponse res)
            throws CodecException, IOException, TauModelException {
        try {
            float gaussianWidth = RevUtil.getFloat("gaussian",
                                                   req,
                                                   Start.getDefaultGaussian());
            res.setContentType("application/zip");
            ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(res.getOutputStream()));
            DataOutputStream dos = new DataOutputStream(zip);
            ArrayList knownEntries = new ArrayList();
            String[] pPhases = {"ttp"};
            TauPUtil tauPTime = TauPUtil.getTauPUtil(HKStack.modelName);
            for(ReceiverFunctionResult cr : results) {
                VelocityEvent event = new VelocityEvent(cr.getEvent());
                VelocityStation sta = new VelocityStation((StationImpl)cr.getChannelGroup()
                        .getChannel1()
                        .getSite()
                        .getStation());
                // 0 for radial, 1 for transverse
                for(int rfType = 0; rfType < 2; rfType++) {
                    String entryName = TOP_ZIP_DIR + "gauss_" + gaussianWidth
                            + "/" + sta.getNetCode() + "." + sta.getCode()
                            + "/" + event.getTime("yyyy_DDD_HH_mm_ss")
                            + (rfType == 0 ? ".itr" : ".itt");
                    String origEntryName = entryName;
                    int j = 2;
                    while(knownEntries.contains(entryName)) {
                        entryName = origEntryName + "." + j;
                        j++;
                    }
                    knownEntries.add(entryName);
                    ZipEntry entry = new ZipEntry(entryName);
                    zip.putNextEntry(entry);
                    LocalSeismogramImpl rfSeis = (LocalSeismogramImpl)(rfType == 0 ? cr.getRadial()
                            : cr.getTransverse());
                    SacTimeSeries sac = FissuresToSac.getSAC(rfSeis,
                                                             cr.getChannelGroup()
                                                                     .getChannel3(),
                                                             cr.getEvent()
                                                                     .getPreferred());
                    // fix orientation to radial and transverse
                    sac.cmpaz = (float)(rfType == 0 ? Rotate.getRadialAzimuth(sta.getLocation(),
                                                                              cr.getEvent()
                                                                                      .getPreferred()
                                                                                      .getLocation())
                            : Rotate.getTransverseAzimuth(sta.getLocation(),
                                                          cr.getEvent()
                                                                  .getPreferred()
                                                                  .getLocation()));
                    sac.cmpinc = 90;
                    // put percent match in user0 and gaussian width in user1
                    sac.user0 = (rfType == 0 ? cr.getRadialMatch()
                            : cr.getTransverseMatch());
                    sac.kuser0 = "% match ";
                    sac.user1 = cr.getGwidth();
                    sac.kuser1 = "gwidth";
                    Arrival[] arrivals = tauPTime.calcTravelTimes(sta,
                                                                  cr.getEvent()
                                                                          .getPreferred(),
                                                                  pPhases);
                    // convert radian per sec ray param into km per sec
                    float kmRayParam = (float)(arrivals[0].getRayParam() / tauPTime.getTauModel()
                            .getRadiusOfEarth());
                    // I don't know why, but this generates slightly wrong
                    // values,
                    // it should be sac.b+10 sec
                    // TauP_SetSac.setSacTHeader(sac, TauP_SetSac.A_HEADER,
                    // arrivals[0]);
                    sac.a = sac.b + 10;
                    sac.user2 = kmRayParam;
                    sac.kuser2 = "rayparam";
                    sac.writeHeader(dos);
                    sac.writeData(dos);
                    dos.flush();
                    zip.closeEntry();
                }
            }
            if(results.size() == 0) {
                // add at least one file so zip doesn't complain
                ZipEntry entry = new ZipEntry(TOP_ZIP_DIR + "no_data.txt");
                zip.putNextEntry(entry);
                dos.writeChars("Sorry, there was no data for your request.\n");
                dos.flush();
                zip.closeEntry();
            }
            zip.close();
        } catch(NoPreferredOrigin e) {
            throw new RuntimeException("Should never happen", e);
        }
    }

    private static final String TOP_ZIP_DIR = "Ears/";

}
