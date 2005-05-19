package edu.sc.seis.receiverFunction.compare;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import edu.sc.seis.fissuresUtil.bag.DistAz;
import edu.sc.seis.fissuresUtil.database.JDBCSequence;
import edu.sc.seis.fissuresUtil.database.JDBCTable;
import edu.sc.seis.fissuresUtil.database.util.TableSetup;

/**
 * @author crotwell Created on May 19, 2005
 */
public class JDBCGriddedResult extends JDBCTable {

    /**
     * @throws SQLException
     */
    public JDBCGriddedResult(JDBCStationResultRef jdbcStationResultRef)
            throws SQLException {
        super("griddedResult", jdbcStationResultRef.getConnection());
        this.jdbcStationResultRef = jdbcStationResultRef;
        seq = new JDBCSequence(conn, "griddedResultSeq");
        TableSetup.setup(this,
                         "edu/sc/seis/receiverFunction/compare/default.props");
    }

    public GriddedResult[] getNearby(double lat, double lon, double degrees)
            throws SQLException {
        ArrayList out = new ArrayList();
        int index = 1;
        ResultSet rs;
        if(lat + degrees > 90) {
            getNorthPole.setDouble(1, lat - degrees);
            rs = getNorthPole.executeQuery();
        } else if(lat - degrees <= -90) {
            getSouthPole.setDouble(1, lat + degrees);
            rs = getSouthPole.executeQuery();
        } else {
            get.setDouble(index++, lat);
            get.setDouble(index++, lon);
            rs = get.executeQuery();
        }
        while(rs.next()) {
            StationResultRef ref = jdbcStationResultRef.extract(rs);
            out.add(new GriddedResult(rs.getDouble("lat"),
                                      rs.getDouble("lon"),
                                      rs.getDouble("h"),
                                      rs.getDouble("herror"),
                                      rs.getDouble("vpvs"),
                                      rs.getDouble("vpvserror"),
                                      rs.getDouble("vp"),
                                      rs.getDouble("vperror"),
                                      ref));
        }
        Iterator it = out.iterator();
        while(it.hasNext()) {
            GriddedResult gr = (GriddedResult)it.next();
            DistAz distaz = new DistAz(lat, lon, gr.lat, gr.lon);
            if (distaz.getDelta() > degrees) {
                it.remove();
            }
        }
        return (GriddedResult[])out.toArray(new GriddedResult[out.size()]);
    }

    PreparedStatement get, getNorthPole, getSouthPole;

    JDBCStationResultRef jdbcStationResultRef;

    JDBCSequence seq;
}