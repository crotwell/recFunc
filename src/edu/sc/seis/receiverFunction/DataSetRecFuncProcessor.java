/**
 * DataSetRecFuncProcessor.java
 *
 * @author Created by Omnicore CodeGuide
 */

package edu.sc.seis.receiverFunction;


import edu.iris.Fissures.AuditInfo;
import edu.iris.Fissures.IfEvent.EventAccessOperations;
import edu.iris.Fissures.IfEvent.NoPreferredOrigin;
import edu.iris.Fissures.IfEvent.Origin;
import edu.iris.Fissures.IfNetwork.Channel;
import edu.iris.Fissures.IfNetwork.ChannelId;
import edu.iris.Fissures.Location;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.fissuresUtil.xml.DataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSetSeismogram;
import edu.sc.seis.fissuresUtil.xml.SeisDataChangeEvent;
import edu.sc.seis.fissuresUtil.xml.SeisDataChangeListener;
import edu.sc.seis.fissuresUtil.xml.SeisDataErrorEvent;
import edu.sc.seis.vsnexplorer.CommonAccess;
import edu.sc.seis.vsnexplorer.configurator.ConfigurationException;
import org.apache.log4j.Logger;
import edu.sc.seis.fissuresUtil.xml.XMLQuantity;
import org.w3c.dom.Element;
import edu.sc.seis.fissuresUtil.xml.DataSetToXML;

public class DataSetRecFuncProcessor implements SeisDataChangeListener {
    DataSetRecFuncProcessor(DataSetSeismogram[] seis,
                            EventAccessOperations event,
                            RecFunc recFunc)
        throws NoPreferredOrigin{
        this.seis = seis;
        this.event = event;
        this.origin = event.get_preferred_origin();
        this.recFunc = recFunc;
        finished = new boolean[seis.length];
        localSeis = new LocalSeismogramImpl[seis.length];
    }


    /**
     * Method error
     *
     * @param    sdce                a  SeisDataErrorEvent
     *
     */
    public void error(SeisDataErrorEvent sdce) {
        error = sdce;
    }

    /**
     * Method finished
     *
     * @param    sdce                a  SeisDataChangeEvent
     *
     */
    public void finished(SeisDataChangeEvent sdce) {
        logger.debug("finished for "+sdce.getSource().getName()+" "+getIndex(sdce.getSource()));
        finished[getIndex(sdce.getSource())] = true;
        for (int i=0; i<finished.length; i++) {
            if ( ! finished[i]) {
                // not all finished yet
                return;
            }
        }
        // must be all finished
        if (error != null) {
            logger.error("problem: ", error.getCausalException());
            return;
        }

        Channel[] channel = new Channel[seis.length];
        for (int i = 0; i < seis.length; i++) {
            channel[i] =
                seis[i].getDataSet().getChannel(seis[i].getRequestFilter().channel_id);
        }

        try {
            ans = recFunc.process(event,
                                  channel,
                                  localSeis);

            Channel chan = channel[0];
            Location staLoc = chan.my_site.my_station.my_location;
            Origin origin = event.get_preferred_origin();
            Location evtLoc = origin.my_location;

            Arrival[] pPhases = CommonAccess.getCommonAccess().getTravelTimes(evtLoc, staLoc, "ttp");
            MicroSecondDate firstP = new MicroSecondDate(origin.origin_time);
            firstP = firstP.add(new TimeInterval(pPhases[0].getTime(), UnitImpl.SECOND));

            TimeInterval shift = recFunc.getShift();
            predictedDSS = new MemoryDataSetSeismogram[2];
            for (int i = 0; i < ans.length; i++) {
                float[] predicted = ans[i].getPredicted();
                String chanCode = (i==0)?"ITR":"ITT"; // ITR for radial
                                                      // ITT for tangential

                ChannelId recFuncChanId = new ChannelId(localSeis[0].channel_id.network_id,
                                                        localSeis[0].channel_id.station_code,
                                                        localSeis[0].channel_id.site_code,
                                                        chanCode,
                                                        localSeis[0].channel_id.begin_time);

                LocalSeismogramImpl predSeis =
                    new LocalSeismogramImpl("recFunc/"+chanCode+"/"+localSeis[0].get_id(),
                                            firstP.subtract(shift).getFissuresTime(),
                                            predicted.length,
                                            localSeis[0].sampling_info,
                                            localSeis[0].y_unit,
                                            recFuncChanId,
                                            predicted);
                predSeis.setName("receiver function "+localSeis[0].channel_id.station_code);
                predictedDSS[i] =
                    new MemoryDataSetSeismogram(predSeis,
                                                "receiver function "+localSeis[0].channel_id.station_code);
                Element alignElement = DataSetToXML.getDocumentBuilder().newDocument().createElement("timeInterval");
                XMLQuantity.insert(alignElement, shift);
                predictedDSS[i].addAuxillaryData("recFunc.alignShift", alignElement);
                AuditInfo[] audit = new AuditInfo[1];
                audit[0] =
                    new AuditInfo("Calculated receiver function",
                                  System.getProperty("user.name"));
                sdce.getSource().getDataSet().addDataSetSeismogram(predictedDSS[i],
                                                                   audit);
            }
            logger.debug("Processing finished OK "+chan.my_site.my_station.name);
        } catch (ConfigurationException ce) {
            logger.error("Unable to get travel time calculator", ce);
            CommonAccess.getCommonAccess().handleException("Unable to get travel time calculator", ce);
        } catch (Exception ee) {
            logger.error("Problem shifting receiver function to align P wave", ee);
        } finally {
            recFuncFinished = true;
        }
    }

    /**
     * Method pushData
     *
     * @param    sdce                a  SeisDataChangeEvent
     *
     */
    public void pushData(SeisDataChangeEvent sdce) {
        if (sdce.getSeismograms().length != 0) {
            localSeis[getIndex(sdce.getSource())] = sdce.getSeismograms()[0];
        } else {
            logger.info("pushData event has zero length localSeismogram array");
        }
    }

    int getIndex(DataSetSeismogram s) {
        for (int i=0; i<seis.length; i++) {
            if (seis[i] == s) {
                return i;
            }
        }
        throw new IllegalArgumentException("Can't find index for this seismogram");
    }

    public boolean isRecFuncFinished() {
        return recFuncFinished;
    }

    public MemoryDataSetSeismogram[] getPredicted() {
        return predictedDSS;
    }

    public SeisDataErrorEvent getError() {
        return error;
    }

    DataSetSeismogram[] seis;

    EventAccessOperations event;

    Origin origin;

    boolean[] finished;

    LocalSeismogramImpl[] localSeis;

    RecFunc recFunc;

    IterDeconResult[] ans = null;

    MemoryDataSetSeismogram[] predictedDSS = null;

    boolean recFuncFinished = false;

    SeisDataErrorEvent error = null;

    static Logger logger = Logger.getLogger(DataSetRecFuncProcessor.class);

}
