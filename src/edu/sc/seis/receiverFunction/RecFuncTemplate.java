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
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

public class RecFuncTemplate {

    public RecFuncTemplate() throws Exception {
        VelocityEngine ve = new VelocityEngine();
        Properties props = new Properties();
        props.put("resource.loader", "class");
        props.put("class.resource.loader.description", "Velocity Classpath Resource Loader");
        props.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        ve.init(props);
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

