package edu.sc.seis.receiverFunction;

import edu.sc.seis.fissuresUtil.sac.SacTimeSeries;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.MalformedURLException;
import edu.sc.seis.fissuresUtil.xml.XMLDataSet;
import java.net.URL;

/**
 * TestIterDecon.java
 *
 *
 * Created: Sat Jun 15 13:47:59 2002
 *
 * @author <a href="mailto:crotwell@localhost">Philip Crotwell</a>
 * @version
 */

public class TestIterDecon {
    public TestIterDecon (){

    }

    public static void main (String[] args) throws Exception {
        if ( args.length != 2) {
            System.out.println("Usage: java TestIterDecon numer.sac denom.sac");
            System.exit(1);
        } // end of if ()

        float gwidth = 2.5f;
        IterDecon decon = new IterDecon(100, true, .001f, gwidth);
        SacTimeSeries num = new SacTimeSeries();
        num.read(args[0]);
        SacTimeSeries denom = new SacTimeSeries();
        denom.read(args[1]);

        float shift = 5;

        IterDeconResult ans = decon.process(num.y, denom.y, num.delta);
        float[] predicted = ans.getPredicted();
        predicted = decon.phaseShift(predicted, shift, num.delta);

        for (int i=0; i<num.y.length; i++) {
            // System.out.println(i+" "+num.y[i]+" "+denom.y[i]+" "+predicted[i]);
        } // end of for (int i=0; i<num.length; i++)




        SacTimeSeries predOut = new SacTimeSeries();
        predOut.y = predicted;
        predOut.npts = predOut.y.length;
        predOut.iftype = predOut.ITIME;
        predOut.leven = predOut.TRUE;
        predOut.idep = predOut.IUNKN;
        predOut.nzyear = num.nzyear;
        predOut.nzjday = num.nzjday;
        predOut.nzhour = num.nzhour;
        predOut.nzmin = num.nzmin;
        predOut.nzsec = num.nzsec;
        predOut.nzmsec = num.nzmsec;
        predOut.knetwk = num.knetwk;
        predOut.kstnm = num.kstnm;
        predOut.kcmpnm = "RRF";
        predOut.delta = num.delta;
        predOut.b = -1*shift;
        predOut.e = predOut.b + (predOut.npts-1)*predOut.delta;
        predOut.depmin = -12345;
        predOut.depmax = -12345;
        predOut.depmen = -12345;
        String filename = "predicted.SAC";
        predOut.write(filename);
        setOSFileExtras(filename);

        float[] residual = ans.getResidual();
        predOut.y = residual;
        filename = "residual.SAC";
        predOut.write(filename);
        setOSFileExtras(filename);

        float[][] corrSave = ans.getCorrSave();
        predOut.y = corrSave[0];
        filename = "correlation0.SAC";
        predOut.write(filename);
        setOSFileExtras(filename);


    } // end of main ()

    private static void setOSFileExtras(String name) {
       try {
            Class fmClass = Class.forName("com.apple.eio.FileManager");
            com.apple.eio.FileManager.setFileCreator(name, GEE_CREATOR_CODE);
        } catch (Exception e) {

       } // end of try-catch
    }

    protected static final int GEE_CREATOR_CODE = 0x47454520;

}// TestIterDecon
