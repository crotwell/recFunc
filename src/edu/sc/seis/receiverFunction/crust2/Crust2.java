/**
 * Crust2.java
 * 
 * @author Philip Crotwell
 */
package edu.sc.seis.receiverFunction.crust2;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import edu.iris.Fissures.IfNetwork.Station;
import edu.sc.seis.TauP.VelocityLayer;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.compare.StationResultRef;

public class Crust2 {

    public Crust2() throws IOException {
        parseTypes();
        parseLatLonModel();
    }

    public String getCode(int longitude, int latitude) {
        if (longitude > 180 || latitude > 90 || longitude < -180 || latitude < -90) {
            throw new RuntimeException("lat/lon out of range: long="+longitude+" lat="+latitude);
        }
        String code = profiles[getLonIndex(longitude)][getLatIndex(latitude)];
        return code;
    }

    public Crust2Profile get(int longitude, int latitude) {
        String code = getCode(longitude, latitude);
        return getByCode(code);
    }

    public Crust2Profile getByCode(String code) {
        return (Crust2Profile)profileMap.get(code);
    }

    public static int[] getClosestLatLon(double lon, double lat) {
        int[] out = new int[2];
        out[0] = (int)Math.round((lon - 1) / 2) * 2 + 1;
        out[1] = (int)Math.round((lat - 1) / 2) * 2 + 1;
        return out;
    }

    public String getClosestCode(double lon, double lat) {
        int[] closest = getClosestLatLon(lon, lat);
        return getCode(closest[0], closest[1]);
    }

    public Crust2Profile getClosest(double lon, double lat) {
        String closest = getClosestCode(lon, lat);
        return getByCode(closest);
    }

    public StationResult getStationResult(Station station) {
        Crust2Profile profile = getClosest(station.my_location.longitude,
                                           station.my_location.latitude);
        return new StationResult(station.my_network.get_id(),
                                 station.get_code(),
                                 (float)profile.getCrustThickness(),
                                 (float)(profile.getPWaveAvgVelocity() / profile.getSWaveAvgVelocity()),
                                 (float)profile.getPWaveAvgVelocity(),
                                 getReference());
    }

    private static final StationResultRef reference = new StationResultRef("Crust2.0",
                                                                           "Bassin, C., Laske, G. and Masters, G., The Current Limits of Resolution for Surface Wave Tomography in North America, EOS Trans AGU, 81, F897, 2000.",
                                                                           "synthesis",
                                                                           "http://mahi.ucsd.edu/Gabi/rem.dir/crust/crust2.html");

    public static StationResultRef getReference() {
        return reference;
    }

    private void parseTypes() throws IOException {
        InputStream inStream = getClass().getClassLoader()
                .getResourceAsStream(DataPrefix + "CNtype2_key.txt");
        BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
        // pull off header lines
        for(int i = 0; i < 5; i++) {
            String line = in.readLine();
        }
        for(int j = 0; j < 360; j++) {
            try {
                String nameLine = in.readLine();
                ArrayList splitName = split(nameLine);
                String code = (String)splitName.get(0);
                String name = nameLine.substring(code.length()).trim();
                ArrayList pLine = split(in.readLine());
                ArrayList sLine = split(in.readLine());
                ArrayList rhoLine = split(in.readLine());
                ArrayList thicknessLine = split(in.readLine());
                VelocityLayer[] layers = new VelocityLayer[8];
                float prevDepth = 0;
                for(int i = 0; i < 8; i++) {
                    layers[i] = new VelocityLayer(i);
                    layers[i].topDepth = prevDepth;
                    if(i != 7) {
                        prevDepth += Float.parseFloat((String)thicknessLine.get(i));
                    } else {
                        // last layer has inf as thickness
                        prevDepth = 6371;
                    }
                    layers[i].botDepth = prevDepth;
                    layers[i].topDensity = Float.parseFloat((String)rhoLine.get(i));
                    layers[i].botDensity = Float.parseFloat((String)rhoLine.get(i));
                    layers[i].topPVelocity = Float.parseFloat((String)pLine.get(i));
                    layers[i].botPVelocity = Float.parseFloat((String)pLine.get(i));
                    layers[i].topSVelocity = Float.parseFloat((String)sLine.get(i));
                    layers[i].botSVelocity = Float.parseFloat((String)sLine.get(i));
                }
                Crust2Profile profile = new Crust2Profile(code, name, layers);
                profileMap.put(code, profile);
            } catch(EOFException e) {
                // end of file?
            }
        }
    }

    private void parseLatLonModel() throws IOException {
        InputStream inStream = getClass().getClassLoader()
                .getResourceAsStream(DataPrefix + "CNtype2.txt");
        BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
        // pull off header line
        ArrayList longitudeList = split(in.readLine());
        int[] longitudes = new int[longitudeList.size()];
        Iterator it = longitudeList.iterator();
        for(int i = 0; i < longitudes.length; i++) {
            longitudes[i] = Integer.parseInt((String)longitudeList.get(i));
        }
        for(int j = 0; j < profiles[0].length; j++) {
            ArrayList values = split(in.readLine());
            int latitude = Integer.parseInt((String)values.get(0));
            for(int i = 0; i < longitudes.length; i++) {
                String code = (String)values.get(i + 1);
                // lat values in file are 90, 88, 86 but center points are 89,
                // 87, 85, so -1
                // lon values in file are -180, -178, -176 but center points are
                // -179, -177, -175, so +1
                profiles[getLonIndex(longitudes[i] + 1)][getLatIndex(latitude - 1)] = code;
            }
        }
    }

    private ArrayList split(String line) {
        ArrayList list = new ArrayList();
        if(line == null) { return list; }
        StringTokenizer token = new StringTokenizer(line);
        while(token.hasMoreTokens()) {
            list.add(token.nextToken());
        }
        return list;
    }

    private static final int getLatIndex(int latitude) {
        return (89 + latitude) / 2;
    }

    private static final int getLonIndex(int longitude) {
        return (180 + longitude) / 2;
    }

    private static String[][] profiles = new String[180][90];

    private static HashMap profileMap = new HashMap();

    private static final String DataPrefix = "edu/sc/seis/receiverFunction/crust2/";
}