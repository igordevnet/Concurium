package com.concurium.server;

import java.lang.reflect.Method;

public record RouteTarget(Object controllerInstance, Method method) {}
