package edu.sc.seis.receiverFunction.server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import edu.iris.Fissures.FissuresException;
import edu.iris.Fissures.IfNetwork.NetworkId;
import edu.iris.Fissures.model.MicroSecondDate;
import edu.iris.Fissures.model.TimeInterval;
import edu.iris.Fissures.model.UnitImpl;
import edu.iris.Fissures.seismogramDC.LocalSeismogramImpl;
import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.fissuresUtil.bag.Cut;
import edu.sc.seis.fissuresUtil.bag.Statistics;
import edu.sc.seis.fissuresUtil.bag.TauPUtil;
import edu.sc.seis.fissuresUtil.database.JDBCTable;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.database.util.TableSetup;
import edu.sc.seis.fissuresUtil.xml.MemoryDataSetSeismogram;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.SumHKStack;
import edu.sc.seis.receiverFunction.compare.StationResult;
import edu.sc.seis.receiverFunction.web.Start;

public class JDBCAmplitude extends JDBCTable {

    public JDBCAmplitude(Connection conn, Properties props) throws Exception {
        super("amplitude", conn);
        StackSummary sum = new StackSummary(getConnection(), props);
        jdbcHKStack = sum.getJDBCHKStack();
        jdbcSumHKStack = sum.getJdbcSummary();
        TableSetup.setup(this,
                        "edu/sc/seis/receiverFunction/server/default.props");
    }

    public void put(int recFuncId, int hkstackId, AmplitudeMeasurement amps)
            throws SQLException {
        int index = 1;
        put.setInt(index++, recFuncId);
        put.setInt(index++, hkstackId);
        put.setFloat(index++, amps.getAmpP());
        put.setFloat(index++, amps.getAmpPs());
        put.setFloat(index++, amps.getAmpPpPs());
        put.setFloat(index++, amps.getAmpPsPs());
        put.executeUpdate();
    }

    public AmplitudeMeasurement getForStation(String netCode, String staCode)
            throws NotFound, SQLException {
        int index = 1;
        getForStation.setString(index, netCode);
        getForStation.setString(index, staCode);
        ResultSet rs = getForStation.executeQuery();
        if(rs.next()) {
            return new AmplitudeMeasurement(rs.getFloat("ampP"),
                                            rs.getFloat("ampPs"),
                                            rs.getFloat("ampPpPs"),
                                            rs.getFloat("ampPsPs"));
        }
        throw new NotFound();
    }

    public void calcAll(String netCode, String staCode) throws SQLException,
            NotFound, FileNotFoundException, FissuresException, IOException,
            TauModelException {
        NetworkId netId = jdbcHKStack.getJDBCChannel()
                .getNetworkTable()
                .getByCode(netCode)[0];
        SumHKStack sumStack = jdbcSumHKStack.getForStation(netId,
                                                           staCode,
                                                           Start.getDefaultGaussian(),
                                                           Start.getDefaultMinPercentMatch(),
                                                           false);
        HKStackIterator iter = jdbcHKStack.getIteratorForStation(netCode,
                                                                 staCode,
                                                                 Start.getDefaultGaussian(),
                                                                 Start.getDefaultMinPercentMatch(),
                                                                 true,
                                                                 true);
        StationResult model = new StationResult(netId,
                                                staCode,
                                                sumStack.getSum()
                                                .getMaxValueH(),
                                                sumStack.getSum()
                                                        .getMaxValueK(),
                                                sumStack.getSum().getAlpha(),
                                                null);
        while(iter.hasNext()) {
            HKStack stack = iter.nextStack();
            LocalSeismogramImpl seis = ((MemoryDataSetSeismogram)stack.getRecFunc()).getCache()[0];
            Arrival[] arrivals = TauPUtil.getTauPUtil()
                    .calcTravelTimes(stack.getChannel().getSite().getStation(),
                                     stack.getOrigin(),
                                     new String[] {"P"});
            float flatRP = (float)arrivals[0].getRayParam() / 6371;
            TimeInterval offsetPs = HKStack.getTimePs(flatRP,
                                                      model.getVp(),
                                                      model.getVpVs(),
                                                      model.getH());
            TimeInterval offsetPpPs = HKStack.getTimePpPs(flatRP,
                                                          model.getVp(),
                                                          model.getVpVs(),
                                                          model.getH());
            TimeInterval offsetPsPs = HKStack.getTimePsPs(flatRP,
                                                          model.getVp(),
                                                          model.getVpVs(),
                                                          model.getH());
            MicroSecondDate timeP = new MicroSecondDate(stack.getOrigin().getOriginTime()).add(new TimeInterval(arrivals[0].getTime(),
                                                                                                            UnitImpl.SECOND));
            MicroSecondDate timePs = timeP.add(offsetPs);
            MicroSecondDate timePpPs = timeP.add(offsetPpPs);
            MicroSecondDate timePsPs = timeP.add(offsetPsPs);
            System.out.println((arrivals[0].getRayParam()/6371) + " " + maxAmp(timeP, seis)
                    + " " + maxAmp(timePs, seis) + " " + maxAmp(timePpPs, seis)
                    + " " + minAmp(timePsPs, seis));
        }
    }

    float maxAmp(MicroSecondDate center, LocalSeismogramImpl seis)
            throws FissuresException {
        return (float)bestAmp(center, seis)[1];
    }

    float minAmp(MicroSecondDate center, LocalSeismogramImpl seis)
            throws FissuresException {
        return (float)bestAmp(center, seis)[0];
    }

    double[] bestAmp(MicroSecondDate center, LocalSeismogramImpl seis)
            throws FissuresException {
        Cut cut = new Cut(center.subtract(halfWindow), center.add(halfWindow));
        Statistics stat = new Statistics(cut.apply(seis).get_as_floats());
        return stat.minMaxMean();
    }

    TimeInterval halfWindow = new TimeInterval(1, UnitImpl.SECOND);

    PreparedStatement put, getForStation;

    JDBCHKStack jdbcHKStack;

    JDBCSummaryHKStack jdbcSumHKStack;

    public static void main(String[] args) throws Exception {
        String netArg = "US";
        String staArg = "NHSC";
        for(int i = 0; i < args.length; i++) {
            if(args[i].equals("-net")) {
                netArg = args[i + 1];
            } else if(args[i].equals("-sta")) {
                staArg = args[i + 1];
            }
        }
        System.out.println("Calc for "+netArg+" "+staArg);
        Properties props = StackSummary.loadProps(args);
        JDBCAmplitude amp = new JDBCAmplitude(StackSummary.initDB(props), props);
        amp.calcAll(netArg, staArg);
    }
}
