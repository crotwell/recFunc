package edu.sc.seis.receiverFunction.web;

import java.io.File;
import java.io.IOException;
import javax.servlet.http.HttpSession;
import edu.sc.seis.fissuresUtil.gmt.ConvertExecute;
import edu.sc.seis.fissuresUtil.gmt.MapCropper;
import edu.sc.seis.fissuresUtil.gmt.PSCoastExecute;
import edu.sc.seis.fissuresUtil.gmt.PSXYExecute;
import edu.sc.seis.sod.velocity.event.VelocityEvent;
import edu.sc.seis.sod.velocity.network.VelocityStation;

public class AzimuthPlot {
    
    public AzimuthPlot() {
        
    }

    public static String plot(VelocityStation station,
                              VelocityEvent[] events,
                              HttpSession session) throws InterruptedException,
            IOException {
        String prefix = "azplot-";
        File psFile = File.createTempFile(prefix, ".ps", getTempDir());
        float staLat = station.getLocation().latitude;
        // -JE proj has trouble at pole
        if (staLat == -90) {staLat = -89.999f;}
        if (staLat == 90) {staLat = 89.999f;}
        String proj = "E" + station.getLongitude() + "/"
                + staLat + "/5i";
        String region = "g";
        PSCoastExecute.createMap(psFile, proj, region, "60g30", "220/220/220", false, 0, .1f);
        double[][] data = new double[events.length][2];
        for(int i = 0; i < data.length; i++) {
            data[i][0] = events[i].getFloatLongitude().floatValue();
            data[i][1] = events[i].getFloatLatitude().floatValue();
        }
        PSXYExecute.addPoints(psFile, proj, region, "c.1i", "0", "0", data);
        // plot station location
        data = new double[1][2];
        data[0][0] = station.getLocation().longitude;
        data[0][1] = station.getLocation().latitude;
        PSXYExecute.addPoints(psFile, proj, region, "t.2i", "0", "0", data);
        PSXYExecute.close(psFile, proj, region);
        String pngFilename = psFile.getName()
                .substring(0, psFile.getName().indexOf(".ps"))
                + ".png";
        File pngFile = new File( getTempDir(), pngFilename);
        ConvertExecute.convert(psFile,
                               pngFile,
                               "-antialias -rotate 90");
        psFile.delete();
        MapCropper cropper = new MapCropper(500, 650, 0, 0, 250, 125, 0, 0);
        cropper.crop(pngFile.getAbsolutePath());
        if(session != null) {
            JFreeChartServletUtilities.registerForDeletion(pngFile,
                                                      session);
        }
        return makeDisplayFilename(pngFilename);
    }
    
    public static String makeDisplayFilename(String name) {
        //return tempSubDir+"/"+name;
        return name;
    }
    
    public static File getTempDir() {
        return tempDir;
    }
    
    private static File tempDir;
    public static final String tempSubDir = "earsTemp";
    static {
        //tempDir = new File(System.getProperty("java.io.tmpdir")+"/"+tempSubDir);
        tempDir = new File(System.getProperty("java.io.tmpdir"));
        tempDir.mkdirs();
    }
}
