package edu.sc.seis.receiverFunction.web;

import java.io.File;
import javax.servlet.http.HttpSession;
import org.jfree.chart.servlet.ServletUtilities;


public class JFreeChartServletUtilities extends ServletUtilities {
    
/** this exists solely because the registerChartForDeletion method is protected in
 * jfreechart.
 * 
 */
    public static void registerForDeletion(File tempFile, 
                                                   HttpSession session) {
        registerChartForDeletion(tempFile, session);
    }
}
