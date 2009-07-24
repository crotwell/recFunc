/**
 * Read data from Zandt and Ammon's Nature paper.
 *
 * @author Created by Omnicore CodeGuide
 */

package edu.sc.seis.receiverFunction.zandtAmmonNature;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.StringTokenizer;

public class ZandtAmmonNature {

    public ZandtAmmonNature() throws IOException {
        load();
    }

    protected void load() throws IOException {
        String sta = "";
        int evt = 0;
        String type = "";
        String quality = "";
        float p = 0;
        float Ps_P = 0;
        float Pm_Ps = 0;

        BufferedReader in = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("edu/sc/seis/receiverFunction/zandtAmmonNature/VpVs2sorttrun.csv")));
        try {
            while(true) {
                String line = in.readLine();
                StringTokenizer st = new StringTokenizer(line, ",");
                for (int i = 0; i < 9; i++) {
                    String token = st.nextToken();
                    switch(i) {
                        case 0:
                            sta = token.substring(0, token.indexOf("-"));
                            evt = Integer.parseInt(token.substring(token.indexOf("-")+1));
                            break;
                        case 1:
                            type = token;
                            break;
                        case 2:
                            quality = token;
                            break;
                        case 3:
                            p = Float.parseFloat(token);
                            break;
                        case 4:
                            Ps_P = Float.parseFloat(token);
                            break;
                        case 8:
                            Pm_Ps = Float.parseFloat(token);
                            break;
                        default:
                            break;
                    }
                    ZAStationResult zaSta = new ZAStationResult(sta, evt, type, quality, p, Ps_P, Pm_Ps);
                    stationResults.put(sta, zaSta);
                }

            }
        } catch(EOFException e) {}
    }

    HashMap stationResults = new HashMap();
}

