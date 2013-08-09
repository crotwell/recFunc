package edu.sc.seis.receiverFunction.web;

import java.io.EOFException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.PropertyConfigurator;
import org.apache.velocity.VelocityContext;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.hibernate.cfg.Configuration;

import edu.iris.Fissures.network.NetworkAttrImpl;
import edu.iris.Fissures.network.NetworkIdUtil;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.database.NotFound;
import edu.sc.seis.fissuresUtil.exceptionHandler.ExceptionInterceptor;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.hibernate.HibernateUtil;
import edu.sc.seis.fissuresUtil.hibernate.NetworkDB;
import edu.sc.seis.fissuresUtil.simple.Initializer;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.rev.FloatQueryParamParser;
import edu.sc.seis.rev.RevUtil;
import edu.sc.seis.rev.Revlet;
import edu.sc.seis.rev.RevletContext;
import edu.sc.seis.rev.hibernate.HibernateDBConfigurer;
import edu.sc.seis.sod.velocity.network.VelocityNetwork;

/**
 * @author crotwell Created on Feb 10, 2005
 */
public class Start {
    
    static {
        GlobalExceptionHandler.add(new ExceptionInterceptor() {
            public boolean handle(String message, Throwable t) {
                if(t instanceof EOFException) {
                    logger.debug("EOFException, ignoring.");
                    return true;
                }
                if(t instanceof SocketException) {
                    logger.debug("SocketException, ignoring.");
                    return true;
                }
                return false;
            }
            
        });
    }

    public static void main(String[] args) throws Exception {
        edu.sc.seis.rev.Start.basicSetup(args); 
        Properties props = Initializer.loadProperties(args);
        PropertyConfigurator.configure(props);
        RecFuncDB.setDataLoc(props.getProperty("cormorant.servers.ears.dataloc", RecFuncDB.getDataLoc()));
        
        edu.sc.seis.rev.Start.runREV(args, 
                                     new RecFuncHandlerProvider(), 
                                     new HibernateDBConfigurer[] {
                                                                  new HibernateDBConfigurer() {
            @Override
            public void configureHibernate() {
                synchronized(HibernateUtil.class) {
                    RecFuncDB.configHibernate(HibernateUtil.getConfiguration());
                }
            }
        }
        });
    }

    public static VelocityContext getDefaultContext() {
        String warning = "<h2>\n"
                + "<p><b>WARNING:</b>I have found an error in our stacking code, and am recalculating all of the stacks. \n"
                + "Until that finishes there will likely be many stations with missing or wrong stacks. Sorry. </p>\n"
                + "</h2>";
        warning = "";
        VelocityContext context = new VelocityContext();
        context.put("header", "<a href=\""
                + RevletContext.getDefault("revBase") + "\"><img src=\""
                + RevletContext.getDefault("staticFiles")
                + "earslogo.png\"/></a><br/>" + warning);
        ArrayList knownGaussians = new ArrayList();
        if(false) {
            knownGaussians.add(new Float(5));
            knownGaussians.add(new Float(2.5f));
            knownGaussians.add(new Float(1));
            knownGaussians.add(new Float(0.7f));
            knownGaussians.add(new Float(0.4f));
        } else {
            knownGaussians.add(new Float(1.0f));
            knownGaussians.add(new Float(2.5f));
        }
        context.put("knownGaussians", knownGaussians);
        return context;
    }

    public static String getDataLoc() {
        return RecFuncDB.getDataLoc();
    }

    public static VelocityNetwork getNetwork(HttpServletRequest req)
            throws NotFound {
        int netDbId = RevUtil.getInt("netdbid", req, -1);
        if(netDbId != -1) {
            return new VelocityNetwork(NetworkDB.getSingleton().getNetwork(netDbId));
        }
        String netCode;
        // also check for netCode to keep google happy
        if (RevUtil.get("netCode", req, null) != null) {
            netCode = RevUtil.get("netCode", req);
        } else {
            netCode = RevUtil.get("netcode", req);
        }
        netCode = netCode.toUpperCase();
        return getNetwork(netCode);
    }

    public static VelocityNetwork getNetwork(String netCode) throws NotFound {
        int netDbId = -1;
        String netCodeNoYear = netCode;
        if(netCodeNoYear != null) {
            // check for XE05 case, but allow G and II to not change
            if(netCodeNoYear.length() > 2) {
                netCodeNoYear = netCodeNoYear.substring(0, 2);
            }
            List<NetworkAttrImpl> nets = NetworkDB.getSingleton().getNetworkByCode(netCodeNoYear);
            for(NetworkAttrImpl networkAttrImpl : nets) {
                if(NetworkIdUtil.toStringNoDates(networkAttrImpl.get_id()).equals(netCode)) {
                    return new VelocityNetwork(networkAttrImpl);
                }
            }
        }
        throw new NotFound();
    }

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Start.class);

    public static float getDefaultGaussian() {
        return 2.5f;
    }

    public static float getDefaultMinPercentMatch() {
        return 80;
    }

}