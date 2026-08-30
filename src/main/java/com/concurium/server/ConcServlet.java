package com.concurium.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

public class ConcServlet extends HttpServlet {

    private final Map<String, RouteTarget> httpRoutes;

    public ConcServlet(Map<String, RouteTarget> httpRoutes) {
        this.httpRoutes = httpRoutes;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String method = req.getMethod().toUpperCase();
        String path = req.getRequestURI();
        String routeKey = method + " " + path;

        RouteTarget target = httpRoutes.get(routeKey);

        if (target != null) {
            try {
                target.method().invoke(target.controllerInstance());

                resp.setContentType("text/plain");
                resp.getWriter().print("Method executed successfully on the server.");
            } catch (Exception e) {
                resp.setStatus(500);
                resp.getWriter().print("Internal Server Error");
            }
        } else {
            resp.setStatus(404);
            resp.getWriter().print("404 - Route Not Found: " + routeKey);
        }
    }

}
