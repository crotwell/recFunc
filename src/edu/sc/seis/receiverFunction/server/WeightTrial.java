package edu.sc.seis.receiverFunction.server;

import java.io.File;
import java.util.ArrayList;
import org.apache.log4j.BasicConfigurator;
import edu.iris.Fissures.IfNetwork.StationId;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.receiverFunction.HKStack;
import edu.sc.seis.receiverFunction.SumHKStack;

/**
 * @author crotwell Created on Oct 16, 2004
 */
public class WeightTrial {

    public static void main(String[] args) {
        BasicConfigurator.configure();
        try {
            float minPercentMatch = 90f;
            float smallestH = 20;
            float weightPs, weightPpPs, weightPpSs;
            // Zhu and Kanamori
            weightPs = 0.7f ; weightPpPs = 0.2f;
            // equals weight
            //weightPs = 1/3f ; weightPpPs = 1/3f;
            // Ps = PpPs + PpSs
            //weightPs = 0.5f ; weightPpPs = 0.25f;
            
            weightPpSs = 1 - weightPs - weightPpPs;
            ArrayList list = JDBCHKStack.calcAndSave(args,
                                                     minPercentMatch,
                                                     false,
                                                     true,
                                                     weightPs,
                                                     weightPpPs,
                                                     weightPpSs);
            System.out.println("Got "+list.size()+" item in stack.");
            HKStack[] stackArray = (HKStack[])list.toArray(new HKStack[0]);
            SumHKStack sumStack = new SumHKStack(stackArray,
                                                 stackArray[0].getChannel(),
                                                 minPercentMatch,
                                                 smallestH);
            StackSummary.saveImage(sumStack,
                                   stackArray[0].getChannel().my_site.my_station.get_id(),
                                   new File("."),
                                   minPercentMatch,
                                   smallestH);
        } catch(Exception e) {
            GlobalExceptionHandler.handle(e);
        }
    }
}