/**
 * RecFuncTemplate.java
 *
 * @author Created by Omnicore CodeGuide
 */

package edu.sc.seis.receiverFunction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

public class RecFuncTemplate {

    static VelocityEngine ve = null;



    public RecFuncTemplate() throws Exception {
        if (ve == null) {
            String loggerName = "Velocity";
            Logger velocityLogger = Logger.getLogger(loggerName);
            Properties props = new Properties();
            props.put("resource.loader", "class");
            props.put("class.resource.loader.description", "Velocity Classpath Resource Loader");
            props.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
            props.put( RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
                      "org.apache.velocity.runtime.log.SimpleLog4JLogSystem" );

            props.put("runtime.log.logsystem.log4j.category", loggerName);
            ve.init(props);
        }
        template = ve.getTemplate("edu/sc/seis/receiverFunction/rfTemplate.vm");

    }

    public void process(VelocityContext context, String filename) throws Exception {
        process(context, new File(filename));
    }

    public void process(VelocityContext context, File file) throws Exception {
        Writer sw = new BufferedWriter(new FileWriter(file));

        template.merge( context, sw );
        sw.close();
    }



    Template template;

}


