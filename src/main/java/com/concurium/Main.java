package com.concurium;

import com.concurium.bootstrap.ConcuriumApplication;
import org.apache.catalina.LifecycleException;

public class Main {


    public static void main(String[] args) throws LifecycleException {
//        Tomcat tomcatServer = new Tomcat();
//
//        tomcatServer.setPort(serverPort);
//        tomcatServer.getConnector();
//        tomcatServer.setBaseDir(new File(".").getAbsolutePath());
//
//        var context = tomcatServer.addContext("", new File(".").getAbsolutePath());
//        Wrapper concServlet = tomcatServer.addServlet(context, "ConcServlet", new ConcServlet());
//        context.addServletMappingDecoded("/hello", "ConcServlet");

        ConcuriumApplication.run(Main.class);
//
//        tomcatServer.start();
//        tomcatServer.getServer().await();

    }
}