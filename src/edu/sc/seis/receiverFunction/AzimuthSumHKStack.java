package edu.sc.seis.receiverFunction;


public class AzimuthSumHKStack {

    public AzimuthSumHKStack(SumHKStack stack, float azimuth, float azWidth) {
        this.stack = stack;
        this.azimuth = azimuth;
        this.azWidth = azWidth;
    }

    SumHKStack stack;

    float azimuth;

    float azWidth;

    public float getAzimuth() {
        return azimuth;
    }

    public float getAzWidth() {
        return azWidth;
    }

    public SumHKStack getStack() {
        return stack;
    }
}
