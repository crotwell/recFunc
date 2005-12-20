package edu.sc.seis.receiverFunction.server;

public class StackComplexityResult {

	public StackComplexityResult(int hksummary_id, float complexity,
			float complexity25, float complexity50, float bestH,
			float bestHStdDev, float bestK, float bestKStdDev, float bestVal,
			float hkCorrelation, float nextH, float nextK, float nextVal,
			float crust2diff) {
		super();
		// TODO Auto-generated constructor stub
		this.hksummary_id = hksummary_id;
		this.complexity = complexity;
		this.complexity25 = complexity25;
		this.complexity50 = complexity50;
		this.bestH = bestH;
		this.bestHStdDev = bestHStdDev;
		this.bestK = bestK;
		this.bestKStdDev = bestKStdDev;
		this.bestVal = bestVal;
		this.hkCorrelation = hkCorrelation;
		this.nextH = nextH;
		this.nextK = nextK;
		this.nextVal = nextVal;
		this.crust2diff = crust2diff;
	}

	int hksummary_id;

	float complexity;

	float complexity25;

	float complexity50;

	float bestH;

	float bestHStdDev;

	float bestK;

	float bestKStdDev;

	float bestVal;

	float hkCorrelation;

	float nextH;

	float nextK;

	float nextVal;

	float crust2diff;
}
