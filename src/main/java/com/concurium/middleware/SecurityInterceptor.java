package com.concurium.middleware;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.concurium.annotations.security.RequireAuth;
import com.concurium.annotations.security.SkipAuth;
import com.concurium.server.RouteDefinition;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Method;

public class SecurityInterceptor implements HandlerInterceptor {

    private final JwtConfiguration jwtConfig;
    private final JWTVerifier verifier;

    public SecurityInterceptor(JwtConfiguration jwtConfig) {
        this.jwtConfig = jwtConfig;

        var verificationBuilder = JWT.require(jwtConfig.getAlgorithm());

        if (jwtConfig.getIssuer() != null) {
            verificationBuilder.withIssuer(jwtConfig.getIssuer());
        }

        verifier = verificationBuilder.build();
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, RouteDefinition target) throws Exception {
        boolean requiresAuth = isAuthRequired(target);

        if(!requiresAuth) {
            return true;
        }

        String authHeader = req.getHeader("Authorization");

        if (authHeader == null || authHeader.isBlank()) {
            resp.setStatus(401);
            resp.setContentType("application/json");
            resp.getWriter().print("{\"error\": \"401 Unauthorized - Token missing\"}");
            return false;
        }

        String token = authHeader.substring(7);

        try {
            DecodedJWT decodedJWT = verifier.verify(token);

            jwtConfig.mapClaims(decodedJWT, req);

            return true;

        } catch (JWTVerificationException exception){
            resp.setStatus(401);
            resp.setContentType("application/json");
            resp.getWriter().print("{\"error\": \"401 Unauthorized - Invalid Token\"}");
            return false;
        }
    }

    private static boolean isAuthRequired(RouteDefinition target) {
        Method method = target.method();
        Class<?> clazz = target.controllerInstance().getClass();

        boolean requiresAuth = false;

        if(method.isAnnotationPresent(RequireAuth.class)) {
            requiresAuth = true;
        } else if (method.isAnnotationPresent(SkipAuth.class)) {
            requiresAuth = false;
        } else if (clazz.isAnnotationPresent(RequireAuth.class)) {
            requiresAuth = true;
        } else if (clazz.isAnnotationPresent(SkipAuth.class)) {
            requiresAuth = false;
        }
        return requiresAuth;
    }
}
