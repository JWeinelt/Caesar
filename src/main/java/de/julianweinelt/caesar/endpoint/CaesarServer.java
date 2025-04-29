package de.julianweinelt.caesar.endpoint;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.auth.PasswordConditions;
import de.julianweinelt.caesar.integration.ServerConnection;
import de.julianweinelt.caesar.storage.APIKeySaver;
import de.julianweinelt.caesar.storage.LocalStorage;
import de.julianweinelt.caesar.util.JWTUtil;
import io.javalin.Javalin;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;

public class CaesarServer {
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final JWTUtil jwt;

    public CaesarServer() {
        jwt = Caesar.getInstance().getJwt();
    }

    public void start() {
        Javalin app = Javalin.create()
                .before(ctx -> ctx.contentType("application/json"))
                .post("/auth", ctx -> {
                    String authBasic = ctx.header("Authorization");
                    String base64 = authBasic.substring(6);

                    byte[] base64DecodedBytes = Base64.getDecoder().decode(base64);
                    String decodedString = new String(base64DecodedBytes);

                    JsonObject o = new JsonObject();
                    o.addProperty("success", true);
                    o.addProperty("token", jwt.token(decodedString.split(":")[0]));
                    ctx.result(o.toString());
                    ctx.status(200);
                })
                .before(ctx -> {
                    String token = ctx.header("Authorization");
                    if (token == null || token.isEmpty()) {
                        ctx.status(401);
                        ctx.result(createErrorResponse(ErrorType.TOKEN_MISSING));
                        ctx.skipRemainingHandlers();
                        return;
                    }
                    token = token.replace("Bearer ", "");
                    if (!jwt.verify(token)) {
                        ctx.status(401);
                        ctx.result(createErrorResponse(ErrorType.TOKEN_INVALID));
                        ctx.skipRemainingHandlers();
                        return;
                    }
                    DecodedJWT decodedJWT = jwt.decode(token);
                    if (decodedJWT.getExpiresAt().before(Date.from(Instant.now()))) {
                        ctx.status(401);
                        ctx.result(createErrorResponse(ErrorType.TOKEN_EXPIRED));
                        ctx.skipRemainingHandlers();
                    }
                })

                // External connections
                .post("/connection", ctx -> {
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();
                    String connectionName = rootObj.get("name").getAsString();
                    String connectionAddress = rootObj.get("address").getAsString();
                    int connectionPort = rootObj.get("port").getAsInt();
                    APIKeySaver.getInstance().saveKey(connectionName);
                    LocalStorage.getInstance().getConnections().add(new ServerConnection(
                            connectionName,
                            connectionAddress,
                            connectionPort)
                    );
                    LocalStorage.getInstance().saveConnections();
                    ctx.result(createSuccessResponse());
                })
                .get("/connection", ctx -> {
                    ctx.result(GSON.toJson(LocalStorage.getInstance().getConnections()));
                })
                .delete("/connection", ctx -> {
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();
                    String connectionName = rootObj.get("name").getAsString();
                    LocalStorage.getInstance().getConnections().removeIf(s -> s.getName().equals(connectionName));
                    LocalStorage.getInstance().saveConnections();
                    ctx.result(createSuccessResponse());
                })

                // User management
                .post("/user", ctx -> {
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();
                    String username = rootObj.get("username").getAsString();
                    String password = rootObj.get("password").getAsString();
                    String discord = rootObj.get("discordID").getAsString();
                    if (username.isEmpty() || password.isEmpty()) {
                        ctx.status(400);
                        ctx.result(createErrorResponse(ErrorType.USERNAME_INVALID));
                        return;
                    }
                    if (username.length() < 3 || username.length() > 16) {
                        ctx.status(400);
                        ctx.result(createErrorResponse(ErrorType.USERNAME_INVALID));
                        return;
                    }
                    PasswordConditions conditions = LocalStorage.getInstance().getData().getPasswordConditions();
                    if (!conditions.checkPassword(password)) {
                        ctx.status(400);
                        ctx.result(createErrorResponse(ErrorType.PASSWORD_INVALID));
                        return;
                    }

                })
                .start(6565);
    }

    public String createErrorResponse(ErrorType type ) {
        JsonObject o = new JsonObject();
        o.addProperty("success", false);
        o.addProperty("reason", type.name());
        return o.toString();
    }

    public String createSuccessResponse(String message) {
        JsonObject o = new JsonObject();
        o.addProperty("success", true);
        o.addProperty("message", message);
        return o.toString();
    }

    public String createSuccessResponse() {
        JsonObject o = new JsonObject();
        o.addProperty("success", true);
        return o.toString();
    }

    public enum ErrorType {
        TOKEN_EXPIRED,
        TOKEN_INVALID,
        TOKEN_MISSING,
        USERNAME_INVALID,
        PASSWORD_INVALID,
        USER_NOT_FOUND,
        USER_DISABLED,
    }
}
