package edu.sc.seis.receiverFunction;

import edu.sc.seis.fissuresUtil.sac.SacTimeSeries;
import java.io.IOException;

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
    
    public static void main (String[] args) throws IOException {
        if ( args.length != 2) {
            System.out.println("Usage: java TestIterDecon numer.sac denom.sac");
            System.exit(1);
        } // end of if ()

        float gwidth = 3;	
        IterDecon decon = new IterDecon(100, true, .001f, gwidth);
        SacTimeSeries num = new SacTimeSeries();
        num.read(args[0]);
        SacTimeSeries denom = new SacTimeSeries();
        denom.read(args[1]);

        float shift = 10;

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
        predOut.write("recfunc.predicted");
        System.out.println("before predicted[0] = "+predOut.y[0]);

        predOut = new SacTimeSeries();
        predOut.read("recfunc.predicted");
        System.out.println("after predicted[0] = "+predOut.y[0]);

        float[] residual = ans.getResidual();
        predOut.y = residual;
        predOut.write("recfunc.residual");

        float[][] corrSave = ans.getCorrSave();
        predOut.y = corrSave[0];
        predOut.write("recfunc.corr");

    } // end of main ()
    
}// TestIterDecon
