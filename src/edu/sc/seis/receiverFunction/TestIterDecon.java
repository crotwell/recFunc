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
	
        IterDecon decon = new IterDecon(100, true, .001f, 2f);
        SacTimeSeries num = new SacTimeSeries();
        num.read(args[0]);
        SacTimeSeries denom = new SacTimeSeries();
        denom.read(args[1]);

        /*	int length = 64;
          float[] num = new float[length];
          float[] denom = new float[length];
          int t=length/8;
          num[t-1] = 6f;
          num[t] = 10f;
          num[t+1] = 7f;
          num[t+8-1] = 1f;
          num[t+8] = 2f;
          num[t+8+1] = 1f;
          num[t+16] = 1f;

          denom[t-1] = 3f;
          denom[t] = 7f;
          denom[t+1] = 4f;
          denom[t+8-1] = 1f;
          denom[t+8] = 3f;
          denom[t+8+1] = 1f;
          denom[t+16] = 1f;
        */

        IterDeconResult ans = decon.process(num.y, denom.y, num.delta);
        float[] predicted = ans.getPredicted();
        for (int i=0; i<num.y.length; i++) {
            // System.out.println(i+" "+num.y[i]+" "+denom.y[i]+" "+predicted[i]);
        } // end of for (int i=0; i<num.length; i++)
        num.y = predicted;
        num.write("recfunc.predicted");

        float[] residual = ans.getResidual();
        num.y = residual;
        num.write("recfunc.residual");

        float[][] corrSave = ans.getCorrSave();
        num.y = corrSave[0];
        num.write("recfunc.corr");

    } // end of main ()
    
}// TestIterDecon
