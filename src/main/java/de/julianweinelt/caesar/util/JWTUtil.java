package de.julianweinelt.caesar.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.storage.Configuration;
import de.julianweinelt.caesar.storage.LocalStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

/**
 * Utility class for handling JWT tokens using the {@link JWT} library.
 * Provides methods for generating, decoding, and verifying JWT tokens.
 * @author Julian Weinelt
 * @version 1.0.0
 */
@SuppressWarnings({"unused", "RedundantSuppression"})
public class JWTUtil {
    private static final Logger log = LoggerFactory.getLogger(JWTUtil.class);

    private final JWTVerifier verifier;

    public JWTUtil() {
        String secret = LocalStorage.getInstance().getData().getJwtSecret();
        if (secret == null) secret = "123456789";
        verifier = JWT.require(Algorithm.HMAC256(secret)).build();
    }

    public static JWTUtil getInstance() {
        return Caesar.getInstance().getJwt();
    }

    /**
     * Generate a JWT token for the given username.
     * @param username the username as a {@link String}
     * @return the generated JWT token as a {@link String} using the {@link JWT} library
     */
    public String token(String username) {
        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();
        calendar.add(Calendar.MINUTE, LocalStorage.getInstance().getData().getTokenExpirationTime());
        Date expiration = calendar.getTime();
        return JWT.create()
                .withSubject(username)
                .withIssuer("caesar")
                .withNotBefore(now)
                .withIssuedAt(now)
                .withExpiresAt(expiration)
                .sign(Algorithm.HMAC256(LocalStorage.getInstance().getData().getJwtSecret()));
    }

    /**
     * Generate a temporary support JWT token for the given user UUID.
     * By default, these tokens are valid for 30 minutes.
     * @param user the user UUID
     * @return the generated support JWT token as a {@link String} using the {@link JWT} library
     */
    public String supportToken(UUID user) {
        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();
        calendar.add(Calendar.MINUTE, Configuration.getInstance().getSupportTokenExpirationTime());
        Date expiration = calendar.getTime();
        return JWT.create()
                .withSubject("support")
                .withClaim("user", user.toString())
                .withClaim("trustedSupportSession", true)
                .withIssuer("caesar")
                .withNotBefore(now)
                .withIssuedAt(now)
                .withExpiresAt(expiration)
                .sign(Algorithm.HMAC256(LocalStorage.getInstance().getData().getJwtSecret()));
    }

    /**
     * Decode the given JWT token.
     * @param token the JWT token as a {@link String}
     * @return the decoded JWT token as a {@link DecodedJWT}, or null if the token is invalid
     */
    public DecodedJWT decode(String token) {
        try {
            return verifier.verify(token);
        } catch (JWTVerificationException e) {
            log.error("Failed to decode JWT token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Verifies if the given {@link String} is a valid JWT token.<br>
     * <b>NOTE: </b>This does not verify any claims, the signature or expiration, it just returns if the {@link String} is a
     * valid token that can be decoded!
     * @param token the JWT token as a {@link String}
     * @return {@code true} if the token is valid, {@code false} otherwise
     */
    public boolean verify(String token) {
        try {
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException e) {
            log.error("Failed to verify JWT token: {}", e.getMessage());
            return false;
        }
    }
}