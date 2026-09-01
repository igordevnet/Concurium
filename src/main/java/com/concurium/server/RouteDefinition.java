package com.concurium.server;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

public record RouteDefinition(Pattern routePattern, Object controllerInstance, Method method) {}
