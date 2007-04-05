package edu.sc.seis.receiverFunction.web;

import java.sql.SQLException;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.rev.locator.StationLocator;
import edu.sc.seis.rev.wrangler.NothingFound;
import edu.sc.seis.sod.ConfigurationException;


public class StationCodeList extends StationList {

    public StationCodeList() throws SQLException, ConfigurationException, Exception {
        super();
        sl = new StationLocator(jdbcChannel.getConnection());
    }
    
    public String getVelocityTemplate(HttpServletRequest req) {
        String fileType = RevUtil.getFileType(req);
        if (fileType.equals(RevUtil.MIME_HTML)) {
            return "stationCodeList.vm";
        } else {
            return super.getVelocityTemplate(req);
        }
    }
    
    public ArrayList getStations(HttpServletRequest req, RevletContext context) throws SQLException,
            NotFound {
        String staCode = RevUtil.get("stacode", req);
        context.put("stacode", staCode);
        if (staCode != null) {
            try {
            return (ArrayList)sl.getStations(staCode);
            } catch (NothingFound e) {
                return new ArrayList();
            }
        }
        return new ArrayList();
    }
    
    private StationLocator sl;
}
