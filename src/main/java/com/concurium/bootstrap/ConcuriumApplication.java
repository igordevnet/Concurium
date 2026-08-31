package com.concurium.bootstrap;

import com.concurium.annotations.*;
import com.concurium.context.ApplicationContext;
import com.concurium.server.ConcServlet;
import com.concurium.server.RouteTarget;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.reflections.Reflections;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

public class ConcuriumApplication {

    private static final int serverPort = 8080;

    private static final List<Class<? extends Annotation>> HTTP_VERBS = List.of(
            Get.class, Post.class, Put.class, Delete.class, Patch.class, Query.class
    );

    private static final List<Class<? extends Annotation>> CLASS_ANNOTATIONS = List.of(
            Service.class, Repository.class, Component.class, Controller.class
    );

    public static void run(Class<?> mainClass) {
        String appPackage = mainClass.getPackageName();
        var reflection = new Reflections(appPackage);

        Set<Class<?>> managedClasses = discoverManagedClasses(reflection);

        ApplicationContext applicationContext = new ApplicationContext();
        applicationContext.initialize(managedClasses);

        var routes = httpScanner(applicationContext, reflection);

        Tomcat tomcatServer = new Tomcat();
        tomcatServer.setPort(serverPort);
        tomcatServer.getConnector();
        tomcatServer.setBaseDir(new File(".").getAbsolutePath());

        var context = tomcatServer.addContext("", new File(".").getAbsolutePath());
        Wrapper concServlet = tomcatServer.addServlet(context, "ConcServlet", new ConcServlet(routes));
        context.addServletMappingDecoded("/*", "ConcServlet");

        try {
            tomcatServer.start();
            tomcatServer.getServer().await();
        } catch (Exception e) {
            throw new RuntimeException("Tomcat failed to start", e);
        }
    }

    private static Map<String, RouteTarget> httpScanner(ApplicationContext applicationContext, Reflections reflection) {
        Set<Class<?>> controllerClasses = reflection.getTypesAnnotatedWith(Controller.class);
        Map<String, RouteTarget> routeRegistry = new HashMap<>();

        for (Class<?> clazz : controllerClasses) {
            try {
                Object controllerInstance = applicationContext.getBean(clazz);

                Controller controllerAnnotation = clazz.getAnnotation(Controller.class);
                String basePath = controllerAnnotation.value();

                for (Method method : clazz.getDeclaredMethods()) {
                    for (Class<? extends Annotation> verbClass : HTTP_VERBS) {
                        if (method.isAnnotationPresent(verbClass)) {
                            Annotation annotation = method.getAnnotation(verbClass);
                            String methodPath = (String) verbClass.getMethod("value").invoke(annotation);

                            String fullPath = (basePath + methodPath).replaceAll("//+", "/");
                            String httpMethod = verbClass.getSimpleName().toUpperCase();
                            String routeKey = httpMethod + " " + fullPath;

                            routeRegistry.put(routeKey, new RouteTarget(controllerInstance, method));
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize route for controller: " + clazz.getName(), e);
            }
        }

        routeRegistry.forEach((key, target) ->
                System.out.println("Mapped: " + key + " -> " + target.controllerInstance().getClass().getSimpleName() + "." + target.method().getName())
        );

        return routeRegistry;
    }

    private static Set<Class<?>> discoverManagedClasses(Reflections reflection) {
        Set<Class<?>> managedClasses = new HashSet<>();
        for (Class<? extends Annotation> annotation : CLASS_ANNOTATIONS) {
            managedClasses.addAll(reflection.getTypesAnnotatedWith(annotation));
        }
        return managedClasses;
    }
}