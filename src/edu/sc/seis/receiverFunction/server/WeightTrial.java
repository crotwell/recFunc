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
            ArrayList list = JDBCHKStack.calcAndSave(args,
                                                     minPercentMatch,
                                                     false,
                                                     true,
                                                     0.7f,
                                                     0.2f,
                                                     0.1f);
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