package com.concurium.server;

import com.concurium.annotations.http.binding.PathVariable;
import com.concurium.annotations.http.binding.RequestBody;
import com.concurium.annotations.http.binding.RequestParam;
import com.concurium.utils.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConcServlet extends HttpServlet {

    private final List<RouteDefinition> httpRoutes;
    private final ObjectMapper objectMapper;

    public ConcServlet(List<RouteDefinition> httpRoutes) {
        this.httpRoutes = httpRoutes;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String method = req.getMethod().toUpperCase();
        String path = req.getRequestURI();
        String routeKey = method + " " + path;

        RouteDefinition targetRoute = null;
        Matcher activeMatcher = null;

        for (RouteDefinition route : httpRoutes) {
            Matcher matcher = route.routePattern().matcher(routeKey);
            if (matcher.matches()) {
                targetRoute = route;
                activeMatcher = matcher;
                break;
            }
        }

        if (targetRoute != null) {
            try {
                var parameters = targetRoute.method().getParameters();
                Object[] args = new Object[parameters.length];

                for (int i = 0; i < parameters.length; i++) {
                    var param = parameters[i];
                    Class<?> paramType = param.getType();

                    if (param.isAnnotationPresent(PathVariable.class)) {
                        PathVariable pathVar = param.getAnnotation(PathVariable.class);
                        String targetName = pathVar.value().isEmpty() ? param.getName() : pathVar.value();

                        String rawValue = activeMatcher.group(targetName);

                        args[i] = convertStringToType(rawValue, paramType);
                    }
                    else if (param.isAnnotationPresent(RequestParam.class)) {
                        RequestParam reqParam = param.getAnnotation(RequestParam.class);
                        String targetName = reqParam.value().isEmpty() ? param.getName() : reqParam.value();

                        String rawValue = req.getParameter(targetName);

                        if (rawValue != null) {
                            args[i] = convertStringToType(rawValue, paramType);
                        }
                    }
                    else if (param.isAnnotationPresent(RequestBody.class)) {
                        args[i] = objectMapper.readValue(req.getInputStream(), paramType);
                    }
                }

                Object result = targetRoute.method().invoke(targetRoute.controllerInstance(), args);

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

        } else{
            resp.setStatus(404);
            resp.getWriter().print("404 - Route Not Found: " + routeKey);
        }
    }

    private Object convertStringToType(String rawValue, Class<?> targetType) {
        if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
            return Integer.parseInt(rawValue);
        } else if (targetType.equals(Long.class) || targetType.equals(long.class)) {
            return Long.parseLong(rawValue);
        } else if (targetType.equals(Boolean.class) || targetType.equals(boolean.class)) {
            return Boolean.parseBoolean(rawValue);
        }
        return rawValue;
    }
}
