package com.concurium.server;

import com.concurium.utils.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

public class ConcServlet extends HttpServlet {

    private final Map<String, RouteTarget> httpRoutes;
    private final ObjectMapper objectMapper;

    public ConcServlet(Map<String, RouteTarget> httpRoutes) {
        this.httpRoutes = httpRoutes;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String method = req.getMethod().toUpperCase();
        String path = req.getRequestURI();
        String routeKey = method + " " + path;

        RouteTarget target = httpRoutes.get(routeKey);

        if (target != null) {
            try {
                Object result = target.method().invoke(target.controllerInstance());

                if (result instanceof ResponseEntity<?> responseEntity) {
                    resp.setStatus(responseEntity.getStatus());
                    resp.setContentType("application/json");

                    responseEntity.getHeaders().forEach(resp::setHeader);

                    if (responseEntity.getBody() != null) {
                        String jsonPayload = objectMapper.writeValueAsString(responseEntity.getBody());
                        resp.getWriter().print(jsonPayload);
                    }
                } else if (result != null) {
                    resp.setStatus(200);
                    resp.setContentType("application/json");
                    resp.getWriter().print(objectMapper.writeValueAsString(result));
                } else {
                    resp.setStatus(204);
                }

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
