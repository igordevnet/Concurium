package com.concurium;

import com.concurium.annotations.Controller;
import com.concurium.annotations.Post;
import com.concurium.bootstrap.ConcuriumApplication;
import com.concurium.server.ConcServlet;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;

import java.io.File;
import java.lang.reflect.Method;

public class Main {

    private static final int serverPort = 8080;

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