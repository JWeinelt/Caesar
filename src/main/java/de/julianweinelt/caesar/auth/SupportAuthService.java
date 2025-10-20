package de.julianweinelt.caesar.auth;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.util.*;
import java.time.*;
import java.util.Base64;

public class SupportAuthService {
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private SecureRandom random = new SecureRandom();
    private PrivateKey serverSigningKey;
    private PublicKey serverPubKey;

    public byte[] generateRandomBytes(int len) {
        byte[] b = new byte[len];
        random.nextBytes(b);
        return b;
    }

    public String base64(byte[] b) { return Base64.getEncoder().encodeToString(b); }

    public Map<String,String> createChallenge(String sessionId, byte[] serverNonce) {
        byte[] challenge = generateRandomBytes(32);
        //saveChallenge(sessionId, challenge, Instant.now().plusSeconds(120));
        Map<String,String> resp = new HashMap<>();
        resp.put("session_id", sessionId);
        resp.put("challenge", base64(challenge));
        resp.put("server_nonce", base64(serverNonce));
        resp.put("expires_at", Instant.now().plusSeconds(300).toString());
        return resp;
    }

    public boolean verifySupporterSignature(byte[] pubKeyBytes, String toSign, String signatureBase64) throws Exception {
        KeyFactory kf = KeyFactory.getInstance("Ed25519");
        X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(pubKeyBytes);
        PublicKey pub = kf.generatePublic(pubSpec);
        Signature sig = Signature.getInstance("Ed25519");
        sig.initVerify(pub);
        sig.update(toSign.getBytes(StandardCharsets.UTF_8));
        byte[] sigBytes = Base64.getDecoder().decode(signatureBase64);
        return sig.verify(sigBytes);
    }

    public String createServerToken(String sessionId, String supporterId, List<String> scopes, int ttlSeconds) throws Exception {
        Map<String,Object> header = Map.of("alg","EdDSA","typ","CESRT");
        Map<String,Object> payload = new HashMap<>();
        payload.put("iss", "caesar-server");
        payload.put("sub", "support-session");
        payload.put("session_id", sessionId);
        payload.put("supporter_id", supporterId);
        payload.put("scopes", scopes);
        payload.put("iat", Instant.now().getEpochSecond());
        payload.put("exp", Instant.now().plusSeconds(ttlSeconds).getEpochSecond());

        String headerB64 = base64UrlEncode(GSON.toJson(header).getBytes(StandardCharsets.UTF_8));
        String payloadB64 = base64UrlEncode(GSON.toJson(payload).getBytes(StandardCharsets.UTF_8));
        String signingInput = headerB64 + "." + payloadB64;

        Signature sig = Signature.getInstance("Ed25519");
        sig.initSign(serverSigningKey);
        sig.update(signingInput.getBytes(StandardCharsets.UTF_8));
        byte[] sigBytes = sig.sign();
        String sigB64 = base64UrlEncode(sigBytes);
        return signingInput + "." + sigB64;
    }

    private String base64UrlEncode(byte[] input) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input);
    }
}
