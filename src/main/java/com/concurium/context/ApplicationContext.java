package com.concurium.context;

import java.util.*;

public class ApplicationContext {

    private final Map<Class<?>, Object> singletonObjects = new HashMap<>();
    private Set<Class<?>> registeredClasses;

    public void initialize(Set<Class<?>> managedClasses) {
        this.registeredClasses = managedClasses;
        Set<Class<?>> inProgress = new HashSet<>();

        for (Class<?> clazz : managedClasses) {
            try {
                if (!clazz.isInterface()) {
                    getOrCreateBean(clazz, inProgress);
                }
            } catch (Exception e) {
                throw new RuntimeException("IoC Container crashed while building: " + clazz.getName(), e);
            }
        }
    }

    public <T> T getBean(Class<T> clazz) {
        return clazz.cast(singletonObjects.get(clazz));
    }

    private Object getOrCreateBean(Class<?> clazz, Set<Class<?>> inProgress) throws Exception {
        if (singletonObjects.containsKey(clazz)) return singletonObjects.get(clazz);
        if (inProgress.contains(clazz)) throw new RuntimeException("Circular dependency: " + clazz.getName());

        if (!registeredClasses.contains(clazz)) {
            throw new RuntimeException("Dependency injection failed: [" + clazz.getName() + "] is not a managed bean.");
        }

        inProgress.add(clazz);

        var constructor = clazz.getDeclaredConstructors()[0];
        Class<?>[] paramTypes = constructor.getParameterTypes();
        Object[] resolvedParams = new Object[paramTypes.length];

        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> paramType = paramTypes[i];

            if (paramType.isInterface()) {
                paramType = resolveInterface(paramType);
            }

            resolvedParams[i] = getOrCreateBean(paramType, inProgress);
        }

        Object instance = constructor.newInstance(resolvedParams);
        singletonObjects.put(clazz, instance);
        inProgress.remove(clazz);

        return instance;
    }

    private Class<?> resolveInterface(Class<?> interfaceType) {
        List<Class<?>> implementations = new ArrayList<>();

        for (Class<?> registeredClass : registeredClasses) {
            if (interfaceType.isAssignableFrom(registeredClass) && !registeredClass.isInterface()) {
                implementations.add(registeredClass);
            }
        }

        if (implementations.isEmpty()) {
            throw new RuntimeException("No implementation found for interface: " + interfaceType.getName());
        }
        if (implementations.size() > 1) {
            throw new RuntimeException("Ambiguous dependency. Multiple implementations found for: " + interfaceType.getName());
        }

        return implementations.get(0);
    }
}