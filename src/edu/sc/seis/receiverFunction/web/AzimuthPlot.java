package edu.sc.seis.receiverFunction.web;

import java.io.File;
import java.io.IOException;
import javax.servlet.http.HttpSession;
import edu.sc.seis.fissuresUtil.gmt.ConvertExecute;
import edu.sc.seis.fissuresUtil.gmt.PSBasemapExecute;
import edu.sc.seis.fissuresUtil.gmt.PSCoastExecute;
import edu.sc.seis.fissuresUtil.gmt.PSXYExecute;
import edu.sc.seis.sod.velocity.event.VelocityEvent;
import edu.sc.seis.sod.velocity.network.VelocityStation;

public class AzimuthPlot {

    public static String plot(VelocityStation station,
                              VelocityEvent[] events,
                              HttpSession session) throws InterruptedException,
            IOException {
        String prefix = "azplot-";
        File psFile = File.createTempFile(prefix, ".ps", tempDir);
        String proj = "E" + station.getLongitude() + "/"
                + station.getLatitude() + "/6i";
        String region = "g";
        //PSXYExecute.open(psFile, proj, region);
        PSCoastExecute.createMap(psFile, proj, region, "60g30", "220/220/220", false, 0, 0);
        double[][] data = new double[events.length][2];
        for(int i = 0; i < data.length; i++) {
            data[i][0] = events[i].getFloatLongitude().floatValue();
            data[i][1] = events[i].getFloatLatitude().floatValue();
        }
        PSXYExecute.addPoints(psFile, proj, region, "c.1i", "0", "0", data);
        // plot station location
        data = new double[1][2];
        data[0][0] = station.my_location.longitude;
        data[0][1] = station.my_location.latitude;
        PSXYExecute.addPoints(psFile, proj, region, "t.2i", "0", "0", data);
        PSXYExecute.close(psFile, proj, region);
        String pngFilename = psFile.getName()
                .substring(0, psFile.getName().indexOf(".ps"))
                + ".png";
        File pngFile = new File(tempDir, pngFilename);
        ConvertExecute.convert(psFile,
                               pngFile,
                               "-antialias -rotate 90");
        //psFile.delete();
        if(session != null) {
            JFreeChartServletUtilities.registerForDeletion(pngFile,
                                                      session);
        }
        return pngFilename;
    }

    static File tempDir = new File(System.getProperty("java.io.tmpdir"));
}
