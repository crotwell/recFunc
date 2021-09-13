package edu.sc.seis.receiverFunction.crust1;

import edu.sc.seis.TauP.VelocityLayer;
import edu.sc.seis.receiverFunction.crust2.Crust2Profile;
import edu.sc.seis.receiverFunction.crust2.LatLonProfile;
import edu.sc.seis.sod.model.common.Location;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;

public class Crust1 {

    public Crust1() {
        try {
            parseLatLonModel();
        } catch(IOException e) {
            throw new RuntimeException("Shouldn't happen", e);
        }
    }


    public Crust1Profile getClosest(double lon, double lat) {
        return profiles[getLatIdx(lat)][getLonIdx(lon)];
    }

    public static int getLatIdx(double lat) {
        return (int)Math.round(90-lat-0.5);
    }
    public static int getLonIdx(double lon) {
        return (int)Math.round(180+lon-0.5);
    }

    public static double[] getClosestLonLat(Location loc) {
        return getClosestLonLat(loc.longitude, loc.latitude);
    }

    public static double[] getClosestLonLat(double lon, double lat) {
        double[] out = new double[2];
        out[0] = (float)Math.round((lon - 0.5)) + 0.5;
        out[1] = (float)Math.round((lat - 0.5)) + 0.5;
        return out;
    }

    void parseLatLonModel() throws IOException {
        InputStream inStream = getClass().getClassLoader()
                .getResourceAsStream(DataPrefix + "crust1.bnds");
        BufferedReader bnds_reader = new BufferedReader(new InputStreamReader(inStream));
        inStream = getClass().getClassLoader()
                .getResourceAsStream(DataPrefix + "crust1.vp");
        BufferedReader vp_reader = new BufferedReader(new InputStreamReader(inStream));
        inStream = getClass().getClassLoader()
                .getResourceAsStream(DataPrefix + "crust1.vs");
        BufferedReader vs_reader = new BufferedReader(new InputStreamReader(inStream));
        inStream = getClass().getClassLoader()
                .getResourceAsStream(DataPrefix + "crust1.rho");
        BufferedReader rho_reader = new BufferedReader(new InputStreamReader(inStream));
        String code = "unknown";
        String name = "";
        for (int latIdx=0; latIdx<profiles.length; latIdx++) {
            for (int lonIdx=0; lonIdx<profiles[latIdx].length; lonIdx++) {
                String[] bndsLine = bnds_reader.readLine().trim().split("\\s+");
                String[] vpLine = vp_reader.readLine().trim().split("\\s+");
                String[] vsLine = vs_reader.readLine().trim().split("\\s+");
                String[] rhoLine = rho_reader.readLine().trim().split("\\s+");
                VelocityLayer[] layers = new VelocityLayer[8];
                float top = 0;
                float bottom = 0;
                for (int i=0; i<layers.length; i++) {
                    top = bottom;
                    if(i != 7) {
                        bottom = -1*Float.parseFloat(bndsLine[i+1]);
                    } else {
                        // last layer has inf as thickness
                        bottom = 6371;
                    }
                    float vp = Float.parseFloat(vpLine[i]);
                    if (vp == 0.0 && top == bottom) { vp = vp_defaults[i]; } // usually due to zero thickness layer
                    float vs = Float.parseFloat(vsLine[i]);
                    if (vs == 0.0 && top == bottom) { vs = vs_defaults[i]; }
                    float rho = Float.parseFloat(rhoLine[i]);
                    if (rho == 0.0 && top == bottom) { rho = rho_defaults[i]; }
                    //System.out.println(i+" "+latIdx+" "+lonIdx+" tb "+top+" "+bottom+" "+bndsLine[i+1]+" "+vp+" "+vs+" "+rho);
                    layers[i] = new VelocityLayer(i, top, bottom,
                            vp, vp, vs, vs, rho, rho);
                }
                Crust1Profile profile = new Crust1Profile(89.5f-latIdx, -179.5f+lonIdx, layers);
                profiles[latIdx][lonIdx] = profile;
            }
        }
    }

    private static float[] vp_defaults = new float[]{1.50f, 3.8f, 2.00f, 3.50f, 5.00f, 5.00f, 5.90f, 6.90f, 8.1f};
    private static float[] vs_defaults = new float[] { 1.50f,  1.94f,  0.55f,  1.79f,  1.79f,  2.70f,  3.70f,  4.05f,  4.49f};
    private static float[] rho_defaults = new float[] { 1.02f,  0.92f,  1.93f,  2.31f,  2.55f,  2.55f,  2.85f,  3.05f,  3.33f};

    private static Crust1Profile[][] profiles = new Crust1Profile[180][360];

    private static HashMap profileMap = new HashMap();

    private static final String DataPrefix = "edu/sc/seis/receiverFunction/crust1/";
}
