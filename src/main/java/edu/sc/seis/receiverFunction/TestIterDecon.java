package edu.sc.seis.receiverFunction;

import java.awt.image.BufferedImage;
import java.io.File;

import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.fissuresUtil.bag.IterDecon;
import edu.sc.seis.fissuresUtil.bag.IterDeconResult;
import edu.sc.seis.fissuresUtil.sac.SacToFissures;
import edu.sc.seis.seisFile.sac.SacConstants;
import edu.sc.seis.seisFile.sac.SacTimeSeries;

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

        IterDeconResult ans = decon.process(num.getY(), denom.getY(), num.getHeader().getDelta());
        float[] predicted = ans.getPredicted();
        predicted = IterDecon.phaseShift(predicted, shift, num.getHeader().getDelta());

        for (int i=0; i<num.getY().length; i++) {
            System.out.println(i+" "+num.getY()[i]+" "+denom.getY()[i]+" "+predicted[i]);
        } // end of for (int i=0; i<num.length; i++)




        SacTimeSeries predOut = new SacTimeSeries();
        predOut.getHeader().setStla(denom.getHeader().getStla());
        predOut.getHeader().setStlo(denom.getHeader().getStlo());
        predOut.setY(predicted);
        predOut.getHeader().setIftype(SacConstants.ITIME);
        predOut.getHeader().setLeven(SacConstants.TRUE);
        predOut.getHeader().setIdep(SacConstants.IUNKN);
        predOut.getHeader().setNzyear(num.getHeader().getNzyear());
        predOut.getHeader().setNzjday(num.getHeader().getNzjday());
        predOut.getHeader().setNzhour(num.getHeader().getNzhour());
        predOut.getHeader().setNzmin(num.getHeader().getNzmin());
        predOut.getHeader().setNzsec(num.getHeader().getNzsec());
        predOut.getHeader().setNzmsec(num.getHeader().getNzmsec());
        predOut.getHeader().setKnetwk(num.getHeader().getKnetwk());
        predOut.getHeader().setKstnm(num.getHeader().getKstnm());
        predOut.getHeader().setKcmpnm("RRF");
        predOut.getHeader().setDelta(num.getHeader().getDelta());
        predOut.getHeader().setB(-1*shift);
        predOut.getHeader().setE(predOut.getHeader().getB() + (predOut.getHeader().getNpts()-1)*predOut.getHeader().getDelta());
        predOut.getHeader().setDepmin(SacConstants.FLOAT_UNDEF);
        predOut.getHeader().setDepmax(SacConstants.FLOAT_UNDEF);
        predOut.getHeader().setDepmen(SacConstants.FLOAT_UNDEF);
        String filename = "predicted.SAC";
        predOut.write(filename);
        setOSFileExtras(filename);

        HKStack stack = new HKStack(new QuantityImpl(6.5f, UnitImpl.KILOMETER_PER_SECOND), 0.06f, gwidth, 1,
                                    new QuantityImpl(10, UnitImpl.KILOMETER), new QuantityImpl(.25f, UnitImpl.KILOMETER), 200,
                                    1.6f,.0025f, 200,
                                    1/3f, 1/3f, 1/3f, SacToFissures.getSeismogram(predOut), new TimeInterval(shift, UnitImpl.SECOND));
        BufferedImage bufSumImage = stack.createStackImage("title");
        File outSumImageFile  = new File("stack.png");
        javax.imageio.ImageIO.write(bufSumImage, "png", outSumImageFile);
        
        float[] residual = ans.getResidual();
        predOut.setY(residual);
        filename = "residual.SAC";
        predOut.write(filename);
        setOSFileExtras(filename);

        float[][] corrSave = ans.getCorrSave();
        predOut.setY(corrSave[0]);
        filename = "correlation0.SAC";
        predOut.write(filename);
        setOSFileExtras(filename);
    } // end of main ()

    private static void setOSFileExtras(String name) {
       try {
            //Class fmClass = Class.forName("com.apple.eio.FileManager");
            //com.apple.eio.FileManager.setFileCreator(name, GEE_CREATOR_CODE);
        } catch (Exception e) {

       } // end of try-catch
    }

    protected static final int GEE_CREATOR_CODE = 0x47454520;

}// TestIterDecon
