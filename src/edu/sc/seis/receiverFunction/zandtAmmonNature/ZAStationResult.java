/**
 * ZAStationResult.java
 *
 * @author Created by Omnicore CodeGuide
 */

package edu.sc.seis.receiverFunction.zandtAmmonNature;

public class ZAStationResult {

    public ZAStationResult(String sta,
                           int evt,
                           String type,
                           String quality,
                           float p,
                           float Ps_P,
                           float Pm_Ps) {
        this.sta = sta;
        this.evt = evt;
        this.type =  type;
        this.quality = quality;
        this.p = p;
        this.Ps_P =  Ps_P;
        this.Pm_Ps = Pm_Ps;
    }

    public double getH(float vp) {
        //E2/(SQRT(1/((6/K2)^2)-D2^2)-SQRT(1/6^2-D2^2))
        return Ps_P/(Math.sqrt(1/((vp/getVpVs(vp))*(vp/getVpVs(vp)))-p*p)
                         -Math.sqrt(1/(vp*vp)-p*p));
    }

    public double getVpVs(float vp) {
        //SQRT((1-6^2*D2^2)*(2*(E2/I2)+1)^2+6^2*D2^2)
        return Math.sqrt((1-vp*vp*p*p)*(2*(Ps_P/Pm_Ps)+1)*(2*(Ps_P/Pm_Ps)+1)+vp*vp*p*p);
    }

    public double getPR(float vp) {
        //(1-0.5*K2^2)/(1-K2^2)
        return (1-0.5*getVpVs(vp)*getVpVs(vp)) /
            (1-getVpVs(vp)*getVpVs(vp));
    }

    String sta;
    int evt;
    String type;
    String quality;
    float p;
    float Ps_P;
    float Pm_Ps;
}

