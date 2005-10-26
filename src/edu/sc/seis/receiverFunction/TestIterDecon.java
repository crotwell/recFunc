package edu.sc.seis.receiverFunction;

import java.awt.image.BufferedImage;
import java.io.File;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.seisFile.sac.SacTimeSeries;
import edu.sc.seis.fissuresUtil.sac.SacToFissures;

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

        float gwidth = 3.0f;
        IterDecon decon = new IterDecon(100, true, .001f, gwidth);
        SacTimeSeries num = new SacTimeSeries();
        num.read(args[0]);
        SacTimeSeries denom = new SacTimeSeries();
        denom.read(args[1]);

        float shift = 10;

        IterDeconResult ans = decon.process(num.y, denom.y, num.delta);
        float[] predicted = ans.getPredicted();
        predicted = IterDecon.phaseShift(predicted, shift, num.delta);

        for (int i=0; i<num.y.length; i++) {
            System.out.println(i+" "+num.y[i]+" "+denom.y[i]+" "+predicted[i]);
        } // end of for (int i=0; i<num.length; i++)




        SacTimeSeries predOut = new SacTimeSeries();
        predOut.stla = denom.stla;
        predOut.stlo = denom.stlo;
        predOut.y = predicted;
        predOut.npts = predOut.y.length;
        predOut.iftype = SacTimeSeries.ITIME;
        predOut.leven = SacTimeSeries.TRUE;
        predOut.idep = SacTimeSeries.IUNKN;
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

        HKStack stack = new HKStack(new QuantityImpl(6.5f, UnitImpl.KILOMETER_PER_SECOND), 0.06f, gwidth, 1,
                                    new QuantityImpl(10, UnitImpl.KILOMETER), new QuantityImpl(.25f, UnitImpl.KILOMETER), 200,
                                    1.6f,.0025f, 200,
                                    1/3f, 1/3f, 1/3f, SacToFissures.getSeismogram(predOut), SacToFissures.getChannel(predOut), new TimeInterval(shift, UnitImpl.SECOND));
        BufferedImage bufSumImage = stack.createStackImage();
        File outSumImageFile  = new File("stack.png");
        javax.imageio.ImageIO.write(bufSumImage, "png", outSumImageFile);
        
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
