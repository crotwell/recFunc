package edu.sc.seis.receiverFunction;

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
    
    public static void main (String[] args) {
	IterDecon decon = new IterDecon(4, true, .001f, 2f);
	int length = 64;
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
	float[] ans = decon.process(num, denom, .25f);
	for (int i=0; i<num.length; i++) {
	    System.out.println(num[i]+" "+denom[i]+" "+ans[i]);
	} // end of for (int i=0; i<num.length; i++)
	
    } // end of main ()
    
}// TestIterDecon
