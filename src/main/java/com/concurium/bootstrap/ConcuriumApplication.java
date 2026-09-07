package com.concurium.bootstrap;

import com.concurium.annotations.bean.Component;
import com.concurium.annotations.bean.Controller;
import com.concurium.annotations.bean.Repository;
import com.concurium.annotations.bean.Service;
import com.concurium.annotations.http.*;
import com.concurium.annotations.security.SecurityBean;
import com.concurium.annotations.web.ExceptionHandler;
import com.concurium.annotations.web.GlobalExceptionHandler;
import com.concurium.context.ApplicationContext;
import com.concurium.middleware.HandlerInterceptor;
import com.concurium.server.ConcServlet;
import com.concurium.server.ExceptionHandlerDefinition;
import com.concurium.server.RouteDefinition;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

public class ConcuriumApplication {

    private static final int serverPort = 8080;

    private static final Logger log = LoggerFactory.getLogger(ConcuriumApplication.class);

    private static final List<Class<? extends Annotation>> HTTP_VERBS = List.of(
            Get.class, Post.class, Put.class, Delete.class, Patch.class, Query.class
    );

    private static final List<Class<? extends Annotation>> CLASS_ANNOTATIONS = List.of(
            Service.class, Repository.class, Component.class, Controller.class, SecurityBean.class, GlobalExceptionHandler.class
    );

    public static void run(Class<?> mainClass) {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        log.info("Starting Concurium Framework...");
        long startTime = System.currentTimeMillis();

        String appPackage = mainClass.getPackageName();
        var reflection = new Reflections(appPackage);

        Set<Class<?>> managedClasses = discoverManagedClasses(reflection);

        ApplicationContext applicationContext = new ApplicationContext();
        applicationContext.initialize(managedClasses);

        var routes = httpScanner(applicationContext, reflection);
        var filterChain = loadFilterChain(applicationContext, reflection);

        Tomcat tomcatServer = new Tomcat();
        tomcatServer.setPort(serverPort);
        tomcatServer.getConnector();
        tomcatServer.setBaseDir(new File(".").getAbsolutePath());

        var context = tomcatServer.addContext("", new File(".").getAbsolutePath());
        Wrapper concServlet = tomcatServer.addServlet(context, "ConcServlet", new ConcServlet(routes, filterChain));
        context.addServletMappingDecoded("/*", "ConcServlet");
        log.info("Loaded {} routes into the registry", routes.size());
        log.info("Tomcat started on port 8080 in {} ms", (System.currentTimeMillis() - startTime));

        try {
            tomcatServer.start();
            tomcatServer.getServer().await();
        } catch (Exception e) {
            throw new RuntimeException("Tomcat failed to start", e);
        }
    }

    private static List<RouteDefinition> httpScanner(ApplicationContext applicationContext, Reflections reflection) {
        Set<Class<?>> controllerClasses = reflection.getTypesAnnotatedWith(Controller.class);
        List<RouteDefinition> routeRegistry = new ArrayList<>();

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

                            String regexPath = fullPath.replaceAll("\\{([^/]+)\\}", "(?<$1>[^/]+)");

                            Pattern routePattern = Pattern.compile("^" + httpMethod + " " + regexPath + "$");

                            routeRegistry.add(new RouteDefinition(routePattern, controllerInstance, method));
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize route for controller: " + clazz.getName(), e);
            }
        }

        return routeRegistry;
    }

    private static List<HandlerInterceptor> loadFilterChain(ApplicationContext context, Reflections reflection) {
        Set<Class<? extends HandlerInterceptor>> filterClasses = reflection.getSubTypesOf(HandlerInterceptor.class);
        List<HandlerInterceptor> filterObjects = new ArrayList<>();

        try {
            for (var interceptor : filterClasses) {
                if (!interceptor.isInterface()) {
                    HandlerInterceptor instance = context.getBean(interceptor);
                    filterObjects.add(instance);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load middleware chain", e);
        }

        return filterObjects;
    }

    private static Map<Class<? extends Throwable>, ExceptionHandlerDefinition> loadExceptionHandlers(
            ApplicationContext context,
            Reflections reflection
    ) {
        Map<Class<? extends Throwable>, ExceptionHandlerDefinition> exceptionRegistry = new HashMap<>();

        Set<Class<?>> errorClasses = reflection.getTypesAnnotatedWith(GlobalExceptionHandler.class);

        for (Class<?> clazz : errorClasses) {
            Object handlerInstance = context.getBean(clazz);

            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(ExceptionHandler.class)) {
                    ExceptionHandler annotation = method.getAnnotation(ExceptionHandler.class);
                    Class<? extends Throwable> targetException = annotation.value();

                    exceptionRegistry.put(targetException, new ExceptionHandlerDefinition(handlerInstance, method));

                    log.info("Mapped exception {} to {}.{}", targetException.getSimpleName(), clazz.getSimpleName(), method.getName());
                }
            }
        }

        return exceptionRegistry;
    }

    private static Set<Class<?>> discoverManagedClasses(Reflections reflection) {
        Set<Class<?>> managedClasses = new HashSet<>();
        for (Class<? extends Annotation> annotation : CLASS_ANNOTATIONS) {
            managedClasses.addAll(reflection.getTypesAnnotatedWith(annotation));
        }
        return managedClasses;
    }
}
