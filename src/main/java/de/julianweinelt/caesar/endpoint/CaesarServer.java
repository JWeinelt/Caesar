package de.julianweinelt.caesar.endpoint;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.auth.PasswordConditions;
import de.julianweinelt.caesar.auth.User;
import de.julianweinelt.caesar.auth.UserManager;
import de.julianweinelt.caesar.auth.UserRole;
import de.julianweinelt.caesar.discord.DiscordBot;
import de.julianweinelt.caesar.integration.ServerConnection;
import de.julianweinelt.caesar.storage.*;
import de.julianweinelt.caesar.util.DatabaseColorParser;
import de.julianweinelt.caesar.util.JWTUtil;
import de.julianweinelt.caesar.util.StringUtil;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.util.JavalinBindException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

public class CaesarServer {
    private static final Logger log = LoggerFactory.getLogger(CaesarServer.class);
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final JWTUtil jwt;

    private Javalin app;

    private boolean isSetupMode;
    private int setupCode;

    public CaesarServer() {
        jwt = Caesar.getInstance().getJwt();
        isSetupMode = false;
    }
    public CaesarServer(boolean setupMode) {
        jwt = Caesar.getInstance().getJwt();
        isSetupMode = setupMode;
    }

    public void start() throws JavalinBindException {
        app = Javalin.create(javalinConfig -> {
            javalinConfig.showJavalinBanner = false;
                })
                .before(ctx -> ctx.contentType("application/json"))

                // For connection checking
                .get("/csetup/checkconnection", ctx -> {
                    if (isSetupMode) {
                        JsonObject o = new JsonObject();
                        o.addProperty("success", true);
                        o.addProperty("setup", true);
                        o.addProperty("useCloudNET", LocalStorage.getInstance().getData().isCloudnetEnabled());
                        ctx.result(o.toString());
                        setupCode = new SecureRandom().nextInt(1000, 9999);
                        log.info("""
                                      _____       _____                            _              _  \s
                                     / / \\ \\     |_   _|                          | |            | | \s
                                    / /| |\\ \\      | |  _ __ ___  _ __   ___  _ __| |_ __ _ _ __ | |_\s
                                   / / | | \\ \\     | | | '_ ` _ \\| '_ \\ / _ \\| '__| __/ _` | '_ \\| __|
                                  / /  |_|  \\ \\   _| |_| | | | | | |_) | (_) | |  | || (_| | | | | |_\s
                                 /_/___(_)___\\_\\ |_____|_| |_| |_| .__/ \\___/|_|   \\__\\__,_|_| |_|\\__|
                                                                 | |                                 \s
                                                                 |_|                                  \
                                """);
                        log.info("Setup code: {}", setupCode);
                        log.info("Please enter the code in the Caesar client to continue setup.");
                        log.info("##################################################################");
                    } else {
                        JsonObject o = new JsonObject();
                        o.addProperty("success", true);
                        o.addProperty("setup", false);
                        o.addProperty("useCloudNET", LocalStorage.getInstance().getData().isCloudnetEnabled());
                        ctx.result(o.toString());
                    }
                })
                .post("/csetup/checkcode", ctx -> {
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();

                    if (isSetupMode) {
                        if (rootObj.get("code").getAsInt() == setupCode) {
                            JsonObject o = new JsonObject();
                            o.addProperty("success", true);
                            ctx.result(o.toString());
                        } else {
                            ctx.result(createErrorResponse(ErrorType.INVALID_SETUP_CODE));
                        }
                    }
                    setupCode = 0;
                })

                // Authentication
                .post("/auth", ctx -> {
                    String authBasic = ctx.header("Authorization");
                    if (authBasic == null || !authBasic.startsWith("Basic ")) {
                        ctx.status(HttpStatus.UNAUTHORIZED); // 401
                        ctx.result(createErrorResponse(ErrorType.INVALID_HEADER));
                        ctx.skipRemainingHandlers();
                        return;
                    }
                    String base64 = authBasic.substring(6);

                    byte[] base64DecodedBytes = Base64.getDecoder().decode(base64);
                    String decodedString = new String(base64DecodedBytes);

                    User user = UserManager.getInstance().getUser(decodedString.split(":")[0]);
                    if (user == null) {
                        ctx.result(createErrorResponse(ErrorType.USER_NOT_FOUND));
                        ctx.status(HttpStatus.UNAUTHORIZED); // 401
                        return;
                    }
                    if (!user.isActive()) {
                        ctx.result(createErrorResponse(ErrorType.USER_DISABLED));
                        ctx.status(HttpStatus.UNAUTHORIZED); // 401
                        return;
                    }
                    if (user.getPassword() != decodedString.split(":")[1].hashCode()) {
                        ctx.result(createErrorResponse(ErrorType.PASSWORD_INVALID));
                        ctx.status(HttpStatus.UNAUTHORIZED); // 401
                        return;
                    }

                    JsonObject o = new JsonObject();
                    o.addProperty("success", true);
                    o.addProperty("token", jwt.token(user.getUsername()));
                    o.addProperty("enforcePasswordChange", user.isNewlyCreated());
                    o.addProperty("setupMode", isSetupMode);
                    o.addProperty("useCloudNET", LocalStorage.getInstance().getData().isCloudnetEnabled());
                    if (LocalStorage.getInstance().getData().isCloudnetEnabled()) {
                        JsonObject c = new JsonObject();
                        String credentials = Base64.getEncoder().encodeToString((LocalStorage.getInstance().getData().getCloudnetUser() + ":" +
                                LocalStorage.getInstance().getData().getCloudnetPassword()).getBytes(StandardCharsets.UTF_8));
                        c.addProperty("credentials", credentials);
                        c.addProperty("host", LocalStorage.getInstance().getData().getCloudnetHost());
                        o.add("cloudnet", c);
                    }
                    ctx.result(o.toString());
                })

                // Check authentication for further requests
                .before(ctx -> {
                    if (ctx.path().contains("csetup") || ctx.path().contains("auth")) return;
                    String token = ctx.header("Authorization");
                    if (token == null || token.isEmpty()) {
                        ctx.status(HttpStatus.FORBIDDEN); // 403
                        ctx.result(createErrorResponse(ErrorType.TOKEN_MISSING));
                        ctx.skipRemainingHandlers();
                        return;
                    }
                    token = token.replace("Bearer ", "");
                    if (!jwt.verify(token)) {
                        ctx.status(HttpStatus.FORBIDDEN); // 403
                        ctx.result(createErrorResponse(ErrorType.TOKEN_INVALID));
                        ctx.skipRemainingHandlers();
                        return;
                    }
                    DecodedJWT decodedJWT = jwt.decode(token);
                    if (decodedJWT.getExpiresAt().before(Date.from(Instant.now()))) {
                        ctx.status(HttpStatus.FORBIDDEN); // 403
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
                .get("/connection", ctx -> ctx.result(GSON.toJson(LocalStorage.getInstance().getConnections())))
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
                        ctx.status(HttpStatus.BAD_REQUEST); // 400
                        ctx.result(createErrorResponse(ErrorType.USERNAME_INVALID));
                        return;
                    }
                    if (username.length() < 3 || username.length() > 16) {
                        ctx.status(HttpStatus.BAD_REQUEST); // 400
                        ctx.result(createErrorResponse(ErrorType.USERNAME_INVALID));
                        return;
                    }
                    PasswordConditions conditions = LocalStorage.getInstance().getData().getPasswordConditions();
                    if (!conditions.checkPassword(password)) {
                        ctx.status(HttpStatus.UNAUTHORIZED); // 401
                        ctx.result(createErrorResponse(ErrorType.PASSWORD_INVALID));
                        return;
                    }
                    UserManager.getInstance().createUser(username, password, discord);
                })
                .delete("/user/{user}", ctx -> {
                    String username = ctx.pathParam("user");
                    if (StringUtil.isUUID(username)) {
                        UserManager.getInstance().deleteUser(UUID.fromString(username));
                        return;
                    }
                    UserManager.getInstance().deleteUser(username);
                })
                .get("/user/{user}", ctx -> {
                    String username = ctx.pathParam("user");
                    if (StringUtil.isUUID(username)) {
                        User u = UserManager.getInstance().getUser(UUID.fromString(username));
                        if (u == null) {
                            ctx.status(HttpStatus.BAD_REQUEST); // 400
                            ctx.result(createErrorResponse(ErrorType.USER_NOT_FOUND));
                            return;
                        }
                        ctx.result(GSON.toJson(u));
                    } else {
                        User u = UserManager.getInstance().getUser(username);
                        if (u == null) {
                            ctx.status(HttpStatus.BAD_REQUEST); // 400
                            ctx.result(createErrorResponse(ErrorType.USER_NOT_FOUND));
                        }
                        ctx.result(GSON.toJson(u));
                    }
                })
                .patch("/user", ctx -> {
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();

                    String username = rootObj.get("username").getAsString();
                    String discord = rootObj.get("discordID").getAsString();
                    boolean enabled = rootObj.get("active").getAsBoolean();

                    User user = UserManager.getInstance().getUser(username);
                    user.setUsername(username);
                    user.setDiscordID(discord);
                    user.setActive(enabled);
                    StorageFactory.getInstance().getUsedStorage().updateUser(user);
                })
                .get("/user", ctx -> {
                    ctx.result(GSON.toJson(UserManager.getInstance().getUsers()));
                })
                .patch("/user/password", ctx -> {
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();

                    String base64Password = rootObj.get("newPassword").getAsString();
                    String base64PasswordOld = rootObj.get("oldPassword").getAsString();

                    byte[] base64DecodedBytes = Base64.getDecoder().decode(base64Password);
                    byte[] base64DecodedBytesOld = Base64.getDecoder().decode(base64PasswordOld);
                    String newPassword = new String(base64DecodedBytes);
                    String oldPassword = new String(base64DecodedBytesOld);

                    String username = rootObj.get("username").getAsString();
                    User user = UserManager.getInstance().getUser(username);
                    if (oldPassword.hashCode() == user.getPassword()) {
                        user.setPassword(newPassword.hashCode());
                        if (user.isNewlyCreated()) user.setNewlyCreated(false); // for first password change
                        StorageFactory.getInstance().getUsedStorage().updateUser(user);
                        ctx.result(createSuccessResponse("Password changed successfully"));
                    } else {
                        ctx.result(createErrorResponse(ErrorType.PASSWORD_INVALID));
                        ctx.status(HttpStatus.FORBIDDEN);
                    }
                })

                // Permission management
                .post("/user/permission", ctx -> {
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();

                    String username = rootObj.get("username").getAsString();
                    String permission = rootObj.get("permission").getAsString();
                    UserManager.getInstance().getUser(username).addPermission(permission);
                    StorageFactory.getInstance().getUsedStorage()
                            .updateUser(UserManager.getInstance().getUser(username));
                })

                .get("/permission", ctx -> {
                    ctx.result(GSON.toJson(UserManager.getInstance().getPermissions()));
                })

                .get("/role", ctx -> {
                    ctx.result(GSON.toJson(UserManager.getInstance().getUserRoles()));
                })
                .post("/role", ctx -> {
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();
                    String name = rootObj.get("name").getAsString();
                    String color = rootObj.get("color").getAsString();
                    UserRole role = new UserRole(name, color, UUID.randomUUID());
                    UserManager.getInstance().addRole(role);
                    ctx.result(createSuccessResponse());
                })
                .post("/role/permission", ctx -> {
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();
                    String key = rootObj.get("permission").getAsString();
                    String role = rootObj.get("role").getAsString();
                    UserManager.getInstance().getRole(role).addPermission(key);
                    StorageFactory.getInstance().getUsedStorage().updateRolePermissions(
                            UserManager.getInstance().getRole(role));
                    ctx.result(createSuccessResponse());
                })

                // Managing Corporate Design for clients
                .patch("/design", ctx -> {
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();
                    Color backgroundColor = DatabaseColorParser.parseColor(rootObj.get("background").getAsString());
                    Color frontColor = DatabaseColorParser.parseColor(rootObj.get("front").getAsString());
                    Color buttonColor = DatabaseColorParser.parseColor(rootObj.get("buttons").getAsString());
                    String logoURL = rootObj.get("logo").getAsString();
                    boolean allowBackgrounds = rootObj.get("allowBackgrounds").getAsBoolean();
                    CorporateDesign design = new CorporateDesign(DatabaseColorParser.parseColor(backgroundColor),
                            DatabaseColorParser.parseColor(frontColor), DatabaseColorParser.parseColor(buttonColor), allowBackgrounds, logoURL);
                    LocalStorage.getInstance().getData().setCorporateDesign(design);
                    LocalStorage.getInstance().saveData();
                    ctx.result(createSuccessResponse());
                })
                .patch("/design/force", ctx -> {
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();
                    boolean enforceCorporate = rootObj.get("enforce").getAsBoolean();
                    LocalStorage.getInstance().getData().setUseCorporateDesign(enforceCorporate);
                    LocalStorage.getInstance().saveData();
                    ctx.result(createSuccessResponse());
                })
                .get("/design", ctx -> ctx.result(GSON.toJson(LocalStorage.getInstance().getData()
                        .getCorporateDesign()))
                )

                // Discord
                .put("/discord/token", ctx -> {
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();

                    String token = rootObj.get("token").getAsString();
                    LocalStorage.getInstance().getData().setDiscordBotToken(token);
                    LocalStorage.getInstance().saveData();
                    DiscordBot.getInstance().restart();
                    ctx.result(createSuccessResponse());
                })

                // Settings
                .put("/config", ctx -> {
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();
                    String key = rootObj.get("key").getAsString();
                    Configuration.ConfigValueType type = Configuration.ConfigValueType.valueOf(rootObj.get("type").getAsString());
                    switch (type) {
                        case INT -> {
                            int val = rootObj.get("value").getAsInt();
                            LocalStorage.getInstance().getData().set(key, val);
                        }
                        case STRING, ONLINE_STATUS -> {
                            String val = rootObj.get("value").getAsString();
                            LocalStorage.getInstance().getData().set(key, val);
                        }
                        case BOOLEAN -> {
                            boolean val = rootObj.get("value").getAsBoolean();
                            LocalStorage.getInstance().getData().set(key, val);
                        }
                        case CORPORATE_DESIGN -> {
                            CorporateDesign design = GSON.fromJson(rootObj.get("value"), CorporateDesign.class);
                            LocalStorage.getInstance().getData().setCorporateDesign(design);
                        }
                        case PASSWORD_CONDITIONS -> {
                            PasswordConditions conditions = GSON.fromJson(rootObj.get("value"), PasswordConditions.class);
                            LocalStorage.getInstance().getData().setPasswordConditions(conditions);
                        }
                    }
                    LocalStorage.getInstance().saveData(true);
                    ctx.result(createSuccessResponse());
                    ctx.status(HttpStatus.OK);
                })
                .get("/config", ctx -> {
                    ctx.result(GSON.toJson(LocalStorage.getInstance().getData()));
                })

                // Important for final setup


                .start(LocalStorage.getInstance().getData().getWebServerPort());
    }

    public void stop() {
        app.stop();
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
        INVALID_HEADER,
        INVALID_SETUP_CODE
    }
}