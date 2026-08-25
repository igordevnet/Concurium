package com.concurium.bootstrap;

import com.concurium.annotations.Controller;
import com.concurium.annotations.Get;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ConcuriumApplication {

    public static void run(Class<?> mainClass) {
        String appPackage = mainClass.getPackageName();
        var reflection = new Reflections(appPackage);
        Set<Class<?>> controllerClasses = reflection.getTypesAnnotatedWith(Controller.class);
        Map<String, Method> routePaths = new HashMap<>();

        controllerClasses.forEach(clazz -> {
            try {
                Object instance = clazz.getDeclaredConstructor().newInstance();
                for(Method method : clazz.getDeclaredMethods()) {
                    if(method.isAnnotationPresent(Get.class)){
                        var path = method.getAnnotation(Get.class);
                        routePaths.computeIfAbsent(path.value(), key -> method);
                    }
                }
            } catch (InstantiationException |
                     IllegalAccessException |
                     InvocationTargetException |
                     NoSuchMethodException e
            ) {
                throw new RuntimeException(e);
            }
        });
    }
}
