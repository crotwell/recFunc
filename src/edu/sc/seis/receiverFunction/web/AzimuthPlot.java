package edu.sc.seis.receiverFunction.web;

import java.io.File;
import java.io.IOException;
import java.util.List;

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

    public static File plot(VelocityStation station,
                            List<VelocityEvent> events,
                            File dir,
                            String prefix) throws IOException, InterruptedException {
        File psFile = new File(dir, prefix+ ".ps");
        float staLat = station.getLocation().latitude;
        // -JE proj has trouble at pole
        if (staLat == -90) {staLat = -89.999f;}
        if (staLat == 90) {staLat = 89.999f;}
        String proj = "E" + station.getLongitude() + "/"
                + staLat + "/5i";
        String region = "g";
        PSCoastExecute.createMap(psFile, proj, region, "60g30", "200/200/200", false, 0, .1f);
        double[][] data = new double[events.size()][2];
        int i=0;
        for (VelocityEvent e : events) {
            data[i][0] = e.getFloatLongitude().floatValue();
            data[i][1] = e.getFloatLatitude().floatValue();
            i++;
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
        File pngFile = new File( dir, pngFilename);
        ConvertExecute.convert(psFile,
                               pngFile,
                               "-antialias -rotate 90");
        psFile.delete();
        MapCropper cropper = new MapCropper(500, 650, 0, 0, 250, 125, 0, 0);
        cropper.crop(pngFile.getAbsolutePath());
        return pngFile;
    }
    
    public static String plot(VelocityStation station,
                              List<VelocityEvent> events,
                              HttpSession session) throws InterruptedException,
            IOException {
        String prefix = "azplot-";
        File pngFile = plot(station, events, getTempDir(), prefix);
        if(session != null) {
            JFreeChartServletUtilities.registerForDeletion(pngFile,
                                                      session);
        }
        return makeDisplayFilename(pngFile.getName());
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
