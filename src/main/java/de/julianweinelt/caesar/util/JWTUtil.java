package de.julianweinelt.caesar.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.storage.LocalStorage;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Date;


public class JWTUtil {
    private static final Logger log = LoggerFactory.getLogger(JWTUtil.class);

    private final SecureRandom random = new SecureRandom();

    private final JWTVerifier verifier;

    public JWTUtil() {
        verifier = JWT.require(Algorithm.HMAC256(LocalStorage.getInstance().getData().getJwtSecret())).build();
    }

    public static JWTUtil getInstance() {
        return Caesar.getInstance().getJwt();
    }

    public String generateSecret(int length) {
        StringBuilder characterPool = new StringBuilder();
        String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        characterPool.append(LETTERS);
        String DIGITS = "0123456789";
        characterPool.append(DIGITS);
        String SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?";
        characterPool.append(SYMBOLS);

        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characterPool.length());
            result.append(characterPool.charAt(index));
        }

        return result.toString();
    }

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

    public DecodedJWT decode(String token) {
        try {
            return verifier.verify(token);
        } catch (JWTVerificationException e) {
            log.error("Failed to verify JWT token: {}", e.getMessage());
            return null;
        }
    }

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