package com.concurium.server;

import java.lang.reflect.Method;

public record ExceptionHandlerDefinition(
        Object instance,
        Method method
) {}