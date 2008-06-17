package edu.sc.seis.receiverFunction.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.w3c.dom.Element;
import edu.iris.Fissures.IfEvent.EventAccessOperations;
import edu.iris.Fissures.IfEvent.EventAttr;
import edu.iris.Fissures.IfEvent.Origin;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.sc.seis.fissuresUtil.database.event.JDBCEventAccess;
import edu.sc.seis.sod.ConfigurationException;
import edu.sc.seis.sod.status.StringTree;
import edu.sc.seis.sod.status.StringTreeLeaf;

public class OriginUpdateProcess {

    public OriginUpdateProcess(Element config) throws ConfigurationException {
        // set up local db connection as we don't want to interfere with the
        // sod database
        Connection earsConn;
        try {
            Class.forName("org.postgresql.Driver").newInstance();
            earsConn = DriverManager.getConnection("jdbc:postgresql:ears",
                                                   "crotwell",
                                                   "");
        } catch(InstantiationException e) {
            throw new ConfigurationException("Trouble connecting to postgres",
                                             e);
        } catch(IllegalAccessException e) {
            throw new ConfigurationException("Trouble connecting to postgres",
                                             e);
        } catch(ClassNotFoundException e) {
            throw new ConfigurationException("Trouble connecting to postgres",
                                             e);
        } catch(SQLException e) {
            throw new ConfigurationException("Trouble connecting to postgres",
                                             e);
        }
        try {
            eventTable = new JDBCEventAccess(earsConn);
            getByTime = earsConn.prepareStatement("SELECT * "
                                              + "FROM origin JOIN time ON ( origin_time_id = time_id ) "
                                              + "JOIN location ON (oring_location_id = loc_id ) "
                                              + "and time_stamp = ? ");
            updateLocation = earsConn.prepareStatement("UPDATE location SET loc_lat = ?, loc_lon = ? WHERE loc_id = ?");
        } catch(SQLException e) {
            throw new ConfigurationException("trouble getting event table", e);
        }
    }

    public StringTree accept(EventAccessOperations eventAccess,
                             EventAttr eventAttr,
                             Origin preferred_origin) throws Exception {
        MicroSecondDate originTime = new MicroSecondDate(preferred_origin.getOriginTime());
        getByTime.setTimestamp(1, originTime.getTimestamp());
        ResultSet rs = getByTime.executeQuery();
        while(rs.next()) {
            int dbid = rs.getInt("origin_id");
            Origin dbOrigin = eventTable.getJDBCOrigin().get(dbid);
            if (dbOrigin.getLocation().latitude == preferred_origin.getLocation().latitude &&
                    dbOrigin.getLocation().longitude == preferred_origin.getLocation().longitude) {
                System.out.println("Match: lat lon equal");
            } else {
                System.out.println("Match: "+dbOrigin.getLocation().latitude +"  "+ preferred_origin.getLocation().latitude 
                                   +"  long "+dbOrigin.getLocation().longitude +"  "+ preferred_origin.getLocation().longitude);
                int loc_dbid = rs.getInt("origin_location_id");
                int index = 1;
                updateLocation.setFloat(index++, preferred_origin.getLocation().latitude);
                updateLocation.setFloat(index++, preferred_origin.getLocation().longitude);
                updateLocation.setFloat(index++, loc_dbid);
                updateLocation.executeUpdate();
            }
        }
        rs.close();
        return new StringTreeLeaf(this, true);
    }

    private JDBCEventAccess eventTable;
    
    private PreparedStatement getByTime, updateLocation;
}
