package com.concurium.middleware;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.concurium.annotations.security.SecurityBean;

import java.util.Date;
import java.util.Map;

@SecurityBean
public class JwtIssuer {

    private final JwtConfiguration jwtConfig;

    public JwtIssuer(JwtConfiguration jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    /**
     * Generates a signed JWT for a given user.
     *
     * @param subject The main identifier (usually User ID or Email)
     * @param customClaims Any extra data (roles, department) to store in the payload
     * @param expireInMs How long the token is valid for (in milliseconds)
     * @return The raw Base64 encoded token string
     */
    public String generateToken(String subject, Map<String, String> customClaims, long expireInMs) {
        long nowMillis = System.currentTimeMillis();

        JWTCreator.Builder builder = JWT.create()
                .withSubject(subject)
                .withIssuedAt(new Date(nowMillis))
                .withExpiresAt(new Date(nowMillis + expireInMs));

        if (jwtConfig.getIssuer() != null) {
            builder.withIssuer(jwtConfig.getIssuer());
        }

        if (customClaims != null) {
            for (Map.Entry<String, String> entry : customClaims.entrySet()) {
                builder.withClaim(entry.getKey(), entry.getValue());
            }
        }

        return builder.sign(jwtConfig.getAlgorithm());
    }
}
