package edu.sc.seis.receiverFunction.server;

public class StackComplexityResult {

	public StackComplexityResult(float complexity,
			float complexity25, float complexity50, float bestH,
			float bestHStdDev, float bestK, float bestKStdDev, float bestVal,
			float hkCorrelation, float nextH, float nextK, float nextVal,
			float crust2diff) {
		super();
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

    
    public float getBestH() {
        return bestH;
    }

    
    public float getBestHStdDev() {
        return bestHStdDev;
    }

    
    public float getBestK() {
        return bestK;
    }

    
    public float getBestKStdDev() {
        return bestKStdDev;
    }

    
    public float getBestVal() {
        return bestVal;
    }

    
    public float getComplexity() {
        return complexity;
    }

    
    public float getComplexity25() {
        return complexity25;
    }

    
    public float getComplexity50() {
        return complexity50;
    }

    
    public float getCrust2diff() {
        return crust2diff;
    }

    
    public float getHkCorrelation() {
        return hkCorrelation;
    }

    
    public int getHksummary_id() {
        return hksummary_id;
    }

    
    public float getNextH() {
        return nextH;
    }

    
    public float getNextK() {
        return nextK;
    }

    
    public float getNextVal() {
        return nextVal;
    }

    
    protected void setHksummary_id(int hksummary_id) {
        this.hksummary_id = hksummary_id;
    }


    
    protected void setComplexity(float complexity) {
        this.complexity = complexity;
    }


    
    protected void setComplexity25(float complexity25) {
        this.complexity25 = complexity25;
    }


    
    protected void setComplexity50(float complexity50) {
        this.complexity50 = complexity50;
    }


    
    protected void setBestH(float bestH) {
        this.bestH = bestH;
    }


    
    protected void setBestHStdDev(float bestHStdDev) {
        this.bestHStdDev = bestHStdDev;
    }


    
    protected void setBestK(float bestK) {
        this.bestK = bestK;
    }


    
    protected void setBestKStdDev(float bestKStdDev) {
        this.bestKStdDev = bestKStdDev;
    }


    
    protected void setBestVal(float bestVal) {
        this.bestVal = bestVal;
    }


    
    protected void setHkCorrelation(float hkCorrelation) {
        this.hkCorrelation = hkCorrelation;
    }


    
    protected void setNextH(float nextH) {
        this.nextH = nextH;
    }


    
    protected void setNextK(float nextK) {
        this.nextK = nextK;
    }


    
    protected void setNextVal(float nextVal) {
        this.nextVal = nextVal;
    }


    
    protected void setCrust2diff(float crust2diff) {
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
