package com.concurium.bootstrap;

import com.concurium.annotations.*;
import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConcuriumApplication {

    public record RouteTarget(Object controllerInstance, Method method) {}

    private static final List<Class<? extends Annotation>> HTTP_VERBS = List.of(
            Get.class, Post.class, Put.class, Delete.class, Patch.class, Query.class
    );

    public static void run(Class<?> mainClass) {
        String appPackage = mainClass.getPackageName();
        var reflection = new Reflections(appPackage);

        var routes = httpScanner(appPackage, reflection);
    }

    private static Map<String, RouteTarget> httpScanner(String appPackage, Reflections reflection) {
        Set<Class<?>> controllerClasses = reflection.getTypesAnnotatedWith(Controller.class);

        Map<String, RouteTarget> routeRegistry = new HashMap<>();

        for (Class<?> clazz : controllerClasses) {
            try {
                Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
                Controller controllerAnnotation = clazz.getAnnotation(Controller.class);
                String basePath = controllerAnnotation.value();

                for (Method method : clazz.getDeclaredMethods()) {
                    for (Class<? extends Annotation> verbClass : HTTP_VERBS) {
                        if (method.isAnnotationPresent(verbClass)) {
                            Annotation annotation = method.getAnnotation(verbClass);
                            String methodPath = (String) verbClass.getMethod("value").invoke(annotation);

                            String fullPath = basePath + methodPath;

                            fullPath = fullPath.replaceAll("//+", "/");

                            String httpMethod = verbClass.getSimpleName().toUpperCase();
                            String routeKey = httpMethod + " " + fullPath;

                            routeRegistry.put(routeKey, new RouteTarget(controllerInstance, method));
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize controllers", e);
            }
        }

        routeRegistry.forEach((key, target) ->
                System.out.println("Mapped: " + key + " -> " + target.controllerInstance().getClass().getSimpleName() + "." + target.method().getName())
        );

        return routeRegistry;
    }
}