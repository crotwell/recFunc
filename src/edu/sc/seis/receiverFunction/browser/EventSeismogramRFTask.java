package edu.sc.seis.receiverFunction.browser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import edu.iris.Fissures.IfEvent.EventAccessOperations;
import edu.iris.Fissures.IfEvent.Origin;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.network.ChannelIdUtil;
import edu.iris.Fissures.network.ChannelImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.IfReceiverFunction.CachedResult;
import edu.sc.seis.IfReceiverFunction.IterDeconConfig;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.fissuresUtil.cache.AbstractJob;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.namingService.FissuresNamingService;
import edu.sc.seis.fissuresUtil.xml.DataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSetSeismogram;
import edu.sc.seis.gee.CommonAccess;
import edu.sc.seis.gee.configurator.ConfigurationException;
import edu.sc.seis.gee.task.DisplayAllTask;
import edu.sc.seis.gee.task.EventSeismogramTask;
import edu.sc.seis.receiverFunction.server.NSRecFuncCache;
import edu.sc.seis.sod.ChannelGroup;
import edu.sc.seis.sod.ChannelGrouper;


/**
 * @author crotwell
 * Created on Oct 19, 2004
 */
public class EventSeismogramRFTask extends EventSeismogramTask {
    
    public void configure(Map params) throws ConfigurationException {
        super.configure(params);
        FissuresNamingService fisName = CommonAccess.getCommonAccess().getFissuresNamingService();
        cache = new NSRecFuncCache(dns, serverName, fisName);
    }
    
    protected void loadSeismograms(EventAccessOperations[] eventAccess,
                                   String bPhase,
                                   double timeBeforeBPhase,
                                   String ePhase,
                                   double timeAfterEPhase,
                                   DisplayAllTask display){
        RecFuncLoader loader = new RecFuncLoader(eventAccess,
                                                 display);
        seisLoader.invokeLater(loader);
    }
    
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(EventSeismogramRFTask.class);
    
    class RecFuncLoader extends AbstractJob {
        RecFuncLoader(EventAccessOperations[] eventAccess,
                      DisplayAllTask display) {
            super("RecFunc Loader");
            this.eventAccess = eventAccess;
            this.display = display;
        }
        
        EventAccessOperations[] eventAccess;
        DisplayAllTask display;
        
        public void runJob() {
            Origin origin = null;
            MicroSecondDate originTime;
            Channel[] channels;
            
            for (int eventNum=0; eventNum<eventAccess.length; eventNum++) {
                try {
                    origin = eventAccess[eventNum].get_preferred_origin();
                    originTime = new MicroSecondDate(origin.origin_time);
                    channels = channelChooserTask.getChannelChooser().getSelectedChannels(originTime);
                    
                    if(channels.length == 0){
                        String mesg = (String)configParams.get("NoStationWithDataMessage");
                        if (mesg == null) {
                            mesg = "Either no stations were selected or the selected stations are not active for this earthquake";
                        }
                        JOptionPane.showMessageDialog(null, mesg,
                                                      "No stations selected", JOptionPane.WARNING_MESSAGE);
                        setFinished();
                        return;
                    }
                    
                    logger.debug("The depth is "+origin.my_location.depth.value);
                    logger.debug("Num Channel->"+channels.length);
                    List failures = new ArrayList();
                    ChannelGroup[] groups = grouper.group(channels, failures);
                    for(int channelNum = 0; channelNum < groups.length; channelNum++){
                        String channelStatus = (channelNum + 1) + "/" + channels.length;
                        setStatus("Loading channel " + channelStatus);// + " for event " + eventStatus);
                        
                        ChannelId[] chanIds = new ChannelId[groups[channelNum].getChannels().length];
                        for(int i = 0; i < chanIds.length; i++) {
                            chanIds[i] = groups[channelNum].getChannels()[i].get_id();
                        }
                        
                        IterDeconConfig[] deconConfig = cache.getCachedConfigs(eventAccess[eventNum].get_preferred_origin(), chanIds);
                        if (deconConfig.length == 0) {
                            continue;
                        }
                        CachedResult result = cache.get(eventAccess[eventNum].get_preferred_origin(), chanIds, deconConfig[0]);
                        
                        if (result != null) {
                            logger.debug("CachedResult is not null "+result.insertTime.date_time);
                            Channel itrChan = new ChannelImpl(new ChannelId(chanIds[0].network_id,
                                                                        chanIds[0].station_code,
                                                                        chanIds[0].site_code,
                                                                        "ITR",
                                                                        chanIds[0].begin_time),
                                                           "ITR_"+groups[channelNum].getChannels()[0].name,
                                                           groups[channelNum].getChannels()[0].an_orientation,
                                                           groups[channelNum].getChannels()[0].sampling_info,
                                                           groups[channelNum].getChannels()[0].effective_time,
                                                           groups[channelNum].getChannels()[0].my_site);
                            MemoryDataSetSeismogram dss = new MemoryDataSetSeismogram((LocalSeismogramImpl)result.radial);
                            populateLocalDataSet(eventAccess[eventNum], dss, itrChan);
                            if (display != null){
                                display.display(dss);
                            }
                        } else {
                            logger.info("No CachedResult for "+
                                        ChannelIdUtil.toString(groups[channelNum].getChannels()[0].get_id()));
                        }
                    }
                } catch(Exception ex) {
                    GlobalExceptionHandler.handle("Problem trying to get seismograms.",
                                                  ex);
                }
            } // end of for (eventNum=0; eventNum<eventAccess.length; eventNum++)
            setFinished();
        }
        
        
    }
    
    ChannelGrouper grouper = new ChannelGrouper();
    
    String dns = "edu/sc/seis";
    
    String interfaceName = "IfReceiverFunction";
    
    String serverName = "Ears";
    
    NSRecFuncCache cache;
}
