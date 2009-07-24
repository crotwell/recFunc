package edu.sc.seis.receiverFunction.server;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.python.core.PyString;

import edu.sc.seis.bag.Bag;
import edu.sc.seis.fissuresUtil.database.ConnMgr;
import edu.sc.seis.fissuresUtil.exceptionHandler.GlobalExceptionHandler;
import edu.sc.seis.fissuresUtil.hibernate.HibernateUtil;
import edu.sc.seis.fissuresUtil.simple.Initializer;
import edu.sc.seis.receiverFunction.hibernate.RecFuncDB;
import edu.sc.seis.rev.admin.Args;
import edu.sc.seis.rev.hibernate.RevDB;
import edu.sc.seis.sod.Start;
import edu.sc.seis.sod.hibernate.SodDB;


public class Admin extends Bag {

    public Admin(Args args) throws IOException {
        super();
        systemState.ps1 = new PyString("ears> ");
        prompt = "Ears Admin";
        exec("from rev import *");
        exec("from recFunc import *");
        if (args.getRecipe() != null && args.getRecipe().exists()) {
            try {
                edu.sc.seis.sod.Start.setConfig(edu.sc.seis.rev.Start.loadXMLFile(args.getRecipe().getPath()));
            } catch(Exception e) {
                GlobalExceptionHandler.handle("Problem with recipe file: "+args.getRecipe().getPath()+", not sod recipe loaded", e);
            }
        } else {
            if (args.getRecipe() != null) {
                logger.warn(args.getRecipe().getPath()+" does not exist, no sod recipe info can be used");
            }
        }
    }

    public static void main(String[] inArgs) throws IOException {
        try {
            BasicConfigurator.configure();
            Args args = new Args(inArgs);
            // get some defaults
            Initializer.loadProps((Start.class).getClassLoader()
                    .getResourceAsStream(DEFAULT_PROPS), props);
            if(args.hasProps()) {
                try {
                    Initializer.loadProps(args.getProps(), props);
                } catch(IOException io) {
                    System.err.println("Unable to load props file: "
                            + io.getMessage());
                    System.err.println("Quitting until the error is corrected");
                    System.exit(1);
                }
            }
            PropertyConfigurator.configure(props);
            ConnMgr.installDbProperties(props, args.getInitialArgs());
            synchronized(HibernateUtil.class) {
                HibernateUtil.setUpFromConnMgr(props, HibernateUtil.DEFAULT_EHCACHE_CONFIG);
                SodDB.configHibernate(HibernateUtil.getConfiguration());
                RevDB.configHibernate(HibernateUtil.getConfiguration());
                RecFuncDB.configHibernate(HibernateUtil.getConfiguration());
            }
            try {
                Connection c = ConnMgr.createConnection();
                ResultSet rs = c.createStatement()
                        .executeQuery("select count(*) from statefulevent");
                rs.next();
                System.out.println("Connected to database: " + ConnMgr.getDB_TYPE()+" at "+ConnMgr.getURL());
                c.close();
            } catch(SQLException e) {
                System.err.println("Unable to connect to database: "+ConnMgr.getURL());
            }
            logger.info("logging configured");
            Admin ic = new Admin(args);
            if(args.getScript() != null) {
                ic.execfile(args.getScript().getPath());
            } else {
                ic.interact();
            }
        } catch(Throwable t) {
            GlobalExceptionHandler.handle(t);
        }
    }

    private static Properties props = System.getProperties();

    protected String historyFilename = ".jline-revAdmin.history";
    
    public static final String DEFAULT_PROPS = "edu/sc/seis/rev/admin/admin.prop";

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Admin.class);
}
