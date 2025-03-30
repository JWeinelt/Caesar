package de.julianweinelt.caesar.core.authentication;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import de.julianweinelt.caesar.core.util.logging.Log;

import javax.annotation.Nullable;
import java.util.Calendar;
import java.util.Date;

public class JWTUtil {
    private final JWTVerifier verifier = JWT.require(Algorithm.HMAC256("t9\"6-4ZWD}y:5U@:9%rt*q:V)8.di84t")).build();

    public String token(final String username) {
        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();
        calendar.add(Calendar.HOUR, 1);
        Date expiry = calendar.getTime();

        return JWT.create()
                .withSubject(username)
                .withIssuer("caesar")
                .withNotBefore(now)
                .withIssuedAt(now)
                .withExpiresAt(expiry)
                .sign(Algorithm.HMAC256("t9\"6-4ZWD}y:5U@:9%rt*q:V)8.di84t"));
    }

    public String extractUsername(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256("t9\"6-4ZWD}y:5U@:9%rt*q:V)8.di84t");

            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("caesar")
                    .build();

            DecodedJWT decodedJWT = verifier.verify(token);

            return decodedJWT.getSubject();
        } catch (JWTVerificationException exception) {
            Log.error("Invalid token provided. Error: {}", exception.getMessage());
            return null;
        }
    }

    @Nullable
    public DecodedJWT verify(String token) {
        try {
            return verifier.verify(token);
        } catch (JWTVerificationException e) {
            Log.warn(e.getMessage());
        }
        return null;
    }
}