package edu.sc.seis.receiverFunction.web;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Properties;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;

import edu.sc.seis.rev.FloatQueryParamParser;
import edu.sc.seis.rev.JettyHandlerProvider;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;

public class RecFuncHandlerProvider extends JettyHandlerProvider {

    public RecFuncHandlerProvider(Properties props) {
        this.props = props;
    }

    @Override
    public ContextHandlerCollection setupJetty() throws Exception {
        ContextHandlerCollection rootHandler = new ContextHandlerCollection();
        RevUtil.populateJetty("/networkList.html",
                              new edu.sc.seis.receiverFunction.web.NetworkList(),
                              rootHandler);
        RevUtil.populateJetty("/stationList.html",
                              new edu.sc.seis.receiverFunction.web.SummaryByNetwork(),
                              rootHandler);
        RevUtil.populateJetty("/stationList.csv",
                              new edu.sc.seis.receiverFunction.web.SummaryByNetwork(),
                              rootHandler);
        RevUtil.populateJetty("/stationCodeList.html",
                              new edu.sc.seis.receiverFunction.web.SummaryByStaCode(),
                              rootHandler);
        RevUtil.populateJetty("/stationCodeList.csv",
                              new edu.sc.seis.receiverFunction.web.SummaryByStaCode(),
                              rootHandler);
        RevUtil.populateJetty("/stationsNearBy.html",
                              new edu.sc.seis.receiverFunction.web.SummaryByPointDistance(),
                              rootHandler);
        RevUtil.populateJetty("/stationsNearBy.csv",
                              new edu.sc.seis.receiverFunction.web.SummaryByPointDistance(),
                              rootHandler);
        RevUtil.populateJetty("/stationLatLonBox.html",
                              new edu.sc.seis.receiverFunction.web.SummaryByLatLonBox(),
                              rootHandler);
        RevUtil.populateJetty("/stationLatLonBox.csv",
                              new edu.sc.seis.receiverFunction.web.SummaryByLatLonBox(),
                              rootHandler);
        RevUtil.populateJetty("/station.html",
                              new edu.sc.seis.receiverFunction.web.Station(),
                              rootHandler);
        RevUtil.populateJetty("/customStack",
                              new edu.sc.seis.receiverFunction.web.CustomStack(),
                              rootHandler);
        RevUtil.populateJetty("/customStackAsXYZ",
                              new edu.sc.seis.receiverFunction.web.CustomStackAsXYZ(),
                rootHandler);
        RevUtil.populateJetty("/summaryHKStack.png",
                              new edu.sc.seis.receiverFunction.web.SummaryHKStackImageServlet(),
                              rootHandler);
        RevUtil.populateJetty("/stationEvent.html",
                              new edu.sc.seis.receiverFunction.web.RFStationEvent(),
                              rootHandler);
        RevUtil.populateJetty("/stationEventSimple.html",
                              new edu.sc.seis.receiverFunction.web.RFStationEventSimple(),
                              rootHandler);
        RevUtil.populateJetty("/hkstackimage.png",
                              new edu.sc.seis.receiverFunction.web.HKStackImageServlet(),
                              rootHandler);
        /*
         * RevUtil.populateJetty("/hkphasestackimage.png",
         * "/hkphasestackimage.png",
         * "edu.sc.seis.receiverFunction.web.HKPhaseStackImage", servletStrings,
         * sh);
         */
        RevUtil.populateJetty("/waveforms.png",
                              new edu.sc.seis.receiverFunction.web.SeismogramImage(),
                              rootHandler);
        RevUtil.populateJetty("/analyticWaveforms.png",
                              new edu.sc.seis.receiverFunction.web.AnalyticPhaseSeismogramImage(),
                              rootHandler);
        RevUtil.populateJetty("/earthquakeHKPlot.png",
                              new edu.sc.seis.receiverFunction.web.EarthquakeHKPlot(),
                              rootHandler);
        RevUtil.populateJetty("/sumHKStackAsXYZ.txt",
                              new edu.sc.seis.receiverFunction.web.SumHKStackAsXYZ(),
                              rootHandler);
        RevUtil.populateJetty("/receiverFunction.zip",
                              new edu.sc.seis.receiverFunction.web.ReceiverFunctionZip(),
                              rootHandler);
        RevUtil.populateJetty("/recordSection.png",
                              new edu.sc.seis.receiverFunction.web.RecordSectionImage(),
                              rootHandler);
        RevUtil.populateJetty("/complexityResidualImage.png",
                              new edu.sc.seis.receiverFunction.web.ComplexityResidualImage(),
                              rootHandler);
        RevUtil.populateJetty("/complexResidualAsXYZ.txt",
                              new edu.sc.seis.receiverFunction.web.ComplexResidualAsXYZ(),
                              rootHandler);
        RevUtil.populateJetty("/synthHKImage.png",
                              new edu.sc.seis.receiverFunction.web.SynthHKImage(),
                              rootHandler);
        RevUtil.populateJetty("/synthHKAsXYZ.txt",
                              new edu.sc.seis.receiverFunction.web.SynthHKAsXYZ(),
                              rootHandler);
        RevUtil.populateJetty("/crust2TypeStats.html",
                              new edu.sc.seis.receiverFunction.web.Crust2TypeStats(),
                              rootHandler);
        RevUtil.populateJetty("/crust2GridCompare.html",
                              new edu.sc.seis.receiverFunction.web.Crust2GridCompare(),
                              rootHandler);
        RevUtil.populateJetty("/crust2GridCompare.txt",
                              new edu.sc.seis.receiverFunction.web.Crust2GridCompare(),
                              rootHandler);
        RevUtil.populateJetty("/eqrate",
                              new edu.sc.seis.receiverFunction.web.EQRateCalc(),
                              rootHandler);
        // jfreechart image servlet
       // RevUtil.populateJetty("/DisplayChart",
       //                       new org.jfree.chart.servlet.DisplayChart(),
       //                       rootHandler);
        RevUtil.populateJetty("/DisplayChart",
                              new MyDisplayChart(),
                              rootHandler);
        RevUtil.populateJetty("/eventSearch.html",
                              new edu.sc.seis.receiverFunction.web.EventSearch(),
                              rootHandler);
        RevUtil.populateJetty( "/network/*",
                               new edu.sc.seis.receiverFunction.rest.Network(),
                               rootHandler);
        RevUtil.populateJetty("/eventReceiverFunction.zip/*",
                              new edu.sc.seis.receiverFunction.web.EventReceiverFunctionZip(),
                              rootHandler);
        RevUtil.populateJetty("/eventSearch.html",
                              new edu.sc.seis.receiverFunction.web.EventSearchResult(),
                              rootHandler);
        // override winkle Event servlet
        RevUtil.populateJetty("/earthquakes/*",
                              new edu.sc.seis.receiverFunction.web.Event(),
                              rootHandler);

        ResourceHandler dataHandler = new ResourceHandler() {

            @Override
            public Resource getResource(String path) throws MalformedURLException {
                logger.info("attempt handle: "+path);
                Resource r = super.getResource(path);
                if (r == null) {
                    logger.info("Didn't handle "+path);
                }
                return r;
            }

        };
        dataHandler.setResourceBase(props.getProperty("ears.dataloc"));
        dataHandler.setDirectoriesListed(true);
        ContextHandler ctx = new ContextHandler("/Data"); /* the server uri path */
        ctx.setHandler(dataHandler);
        rootHandler.addHandler(ctx);

        edu.sc.seis.receiverFunction.web.IndexPage ip = new edu.sc.seis.receiverFunction.web.IndexPage();
        RevUtil.populateJetty("/", ip, rootHandler);
        RevUtil.populateJetty("/index.html", ip, rootHandler);
        Revlet.addStandardQueryParam(new FloatQueryParamParser("gaussian", Start.getDefaultGaussian()));
        Revlet.addStandardQueryParam(new FloatQueryParamParser("minPercentMatch", Start.getDefaultMinPercentMatch()));
        return rootHandler;
    }

    @Override
    public List<Handler> additionalHandlers() {
        List<Handler> out = super.additionalHandlers();
        return out;
    }

    protected Properties props;

    public static final String WINKLE = "/winkle";

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RecFuncHandlerProvider.class);
}
