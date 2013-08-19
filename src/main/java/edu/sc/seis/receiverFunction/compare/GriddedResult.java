package edu.sc.seis.receiverFunction.compare;

/**
 * @author crotwell Created on May 19, 2005
 */
public class GriddedResult {

    /**
     *
     */
    public GriddedResult(double lat,
                         double lon,
                         double h,
                         double herror,
                         double vpvs,
                         double vpvserror,
                         double vp,
                         double vperror,
                         StationResultRef ref) {
        this.lat = lat;
        this.lon = lon;
        this.h = h;
        this.herror = herror;
        this.vpvs = vpvs;
        this.vpvserror = vpvserror;
        this.vp = vp;
        this.vp = vperror;
        this.ref = ref;
    }

    double lat, lon, h, herror, vpvs, vpvserror, vp, vperror;

    StationResultRef ref;
}