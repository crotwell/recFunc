package edu.sc.seis.receiverFunction.web;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.iris.Fissures.Area;
import edu.iris.Fissures.model.BoxAreaImpl;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.QuantityImpl;
import edu.iris.Fissures.model.UnitImpl;
import edu.sc.seis.fissuresUtil.cache.CacheEvent;
import edu.sc.seis.fissuresUtil.time.MicroSecondTimeRange;
import edu.sc.seis.fissuresUtil.flow.querier.EventFinderQuery;
import edu.sc.seis.fissuresUtil.hibernate.EventDB;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.sod.velocity.event.VelocityEvent;

public class EventSearchResult extends XMLRevlet {

    public EventSearchResult() throws SQLException {
    }

    public RevletContext getContext(HttpServletRequest req,
                                    HttpServletResponse res) throws Exception {
        MicroSecondDate begin = RevUtil.getDate("begin", req);
        MicroSecondDate end = RevUtil.getDate("end", req);
        QuantityImpl minDepth = new QuantityImpl(RevUtil.getFloat("minDepth",
                                                                  req,
                                                                  0),
                                                 UnitImpl.KILOMETER);
        QuantityImpl maxDepth = new QuantityImpl(RevUtil.getFloat("maxDepth",
                                                                  req,
                                                                  1000),
                                                 UnitImpl.KILOMETER);
        float minLat = RevUtil.getFloat("minLat", req, -90);
        float maxLat = RevUtil.getFloat("maxLat", req, 90);
        float minLon = RevUtil.getFloat("minLon", req, -180);
        float maxLon = RevUtil.getFloat("maxLon", req, 180);
        float minMag = RevUtil.getFloat("minMag", req, -9999);
        float maxMag = RevUtil.getFloat("maxMag", req, 9999);
        Area area = new BoxAreaImpl(minLat, maxLat, minLon, maxLon);
        EventFinderQuery eventFinderQuery = new EventFinderQuery();
        eventFinderQuery.setArea(area);
        eventFinderQuery.setMinDepth(minDepth.value);
        eventFinderQuery.setMaxDepth(maxDepth.value);
        eventFinderQuery.setMinMag(minMag);
        eventFinderQuery.setMaxMag(maxMag);
        eventFinderQuery.setTime(new MicroSecondTimeRange(begin, end));
        List<CacheEvent> events = EventDB.getSingleton().query(eventFinderQuery);
        List<VelocityEvent> eventList = new ArrayList<VelocityEvent>();
        for(CacheEvent event : events) {
            VelocityEvent ve = new VelocityEvent(event);
            eventList.add(ve);
        }
        String mimeType = RevUtil.getFileType(req);
        RevletContext context;
        String templateName = "eventList.vm";
        if (mimeType.equals(RevUtil.MIME_XML)) {
            templateName = "eventListXML.vm";
            res.addHeader("Content-Disposition", "inline; filename="+"eventList.xml");
        } else if (mimeType.equals(RevUtil.MIME_CSV)) {
            templateName = "eventListCSV.vm";
            res.addHeader("Content-Disposition", "inline; filename="+"eventList.csv");
        }

        context = new RevletContext(templateName,
                                    Start.getDefaultContext());
        context.put("eventList", eventList);
        return context;
    }
}
