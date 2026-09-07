package com.concurium.middleware;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.http.HttpServletRequest;

public interface JwtConfiguration {
    Algorithm getAlgorithm();

    String getIssuer();

    void mapClaims(DecodedJWT jwt, HttpServletRequest req);
}