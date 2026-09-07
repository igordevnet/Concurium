package com.concurium.middleware;

import com.concurium.server.RouteDefinition;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface HandlerInterceptor {
    boolean preHandle(HttpServletRequest req, HttpServletResponse resp, RouteDefinition target) throws Exception;
}
