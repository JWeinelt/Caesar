package de.julianweinelt.caesar.endpoint;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.*;
import de.julianweinelt.caesar.Caesar;
import de.julianweinelt.caesar.auth.*;
import de.julianweinelt.caesar.discord.DiscordBot;
import de.julianweinelt.caesar.discord.ticket.TicketStatus;
import de.julianweinelt.caesar.discord.ticket.TicketType;
import de.julianweinelt.caesar.integration.ServerConnection;
import de.julianweinelt.caesar.storage.APIKeySaver;
import de.julianweinelt.caesar.storage.Configuration;
import de.julianweinelt.caesar.storage.LocalStorage;
import de.julianweinelt.caesar.storage.StorageFactory;
import de.julianweinelt.caesar.util.DatabaseColorParser;
import de.julianweinelt.caesar.util.JWTUtil;
import de.julianweinelt.caesar.util.StringUtil;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.util.JavalinBindException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@SuppressWarnings("SpellCheckingInspection")
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
        app = Javalin.create(javalinConfig -> javalinConfig.showJavalinBanner = false)
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
                    if (serviceUnavailable(ctx)) return;
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();

                    if (isSetupMode) {
                        if (rootObj.get("code").getAsInt() == setupCode) {
                            JsonObject o = new JsonObject();
                            o.addProperty("success", true);
                            isSetupMode = false;
                            ctx.result(o.toString());
                        } else {
                            ctx.result(createErrorResponse(ErrorType.INVALID_SETUP_CODE));
                        }
                    }
                    setupCode = 0;
                })

                // Authentication
                .post("/auth", ctx -> {
                    if (serviceUnavailable(ctx)) return;
                    String authBasic = ctx.header("Authorization");
                    if (authBasic == null || !authBasic.startsWith("Basic ")) {
                        ctx.status(HttpStatus.UNAUTHORIZED); // 401
                        ctx.result(createErrorResponse(ErrorType.INVALID_HEADER)).skipRemainingHandlers();
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
                    o.addProperty("userID", user.getUuid().toString());
                    o.addProperty("useCloudNET", LocalStorage.getInstance().getData().isCloudnetEnabled());
                    if (LocalStorage.getInstance().getData().isCloudnetEnabled()) {
                        JsonObject c = new JsonObject();
                        String credentials = Base64.getEncoder().encodeToString((LocalStorage.getInstance().getData().getCloudnetUser() + ":" +
                                LocalStorage.getInstance().getData().getCloudnetPassword()).getBytes(StandardCharsets.UTF_8));
                        c.addProperty("credentials", credentials);
                        c.addProperty("host", LocalStorage.getInstance().getData().getCloudnetHost());
                        o.add("cloudnet", c);
                    }
                    o.addProperty("chatServer", LocalStorage.getInstance().getData().getChatServerPort());
                    JsonObject features = new JsonObject();
                    features.addProperty("chat", LocalStorage.getInstance().getData().isUseChat());
                    features.addProperty("mail", LocalStorage.getInstance().getData().isUseMailClient());
                    features.addProperty("support", LocalStorage.getInstance().getData().isUseSupport());
                    features.addProperty("files", LocalStorage.getInstance().getData().isEnableFileBrowser());
                    o.add("features", features);
                    JsonArray permissions = new JsonArray();
                    for (String s : user.getPermissions()) permissions.add(s);
                    o.add("permissions", permissions);
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
                    if (lackingPermissions(ctx, "caesar.connections.create")) return;
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();
                    String connectionName = rootObj.get("name").getAsString();
                    String connectionAddress = rootObj.get("address").getAsString();
                    int connectionPort = rootObj.get("port").getAsInt();
                    boolean encrypted = rootObj.get("encrypted").getAsBoolean();
                    UUID uuid = UUID.fromString(rootObj.get("uuid").getAsString());
                    byte[] key = CaesarLinkServer.getInstance().generateKey();
                    APIKeySaver.getInstance().saveKey(connectionName, key);
                    LocalStorage.getInstance().getConnections().add(new ServerConnection(
                            uuid,
                            connectionName,
                            connectionAddress,
                            connectionPort, encrypted)
                    );
                    LocalStorage.getInstance().saveConnections();
                    JsonObject result = new JsonObject();
                    result.addProperty("success", true);
                    result.addProperty("key", new String(key));
                    ctx.result(result.toString());
                })
                .get("/connection", ctx -> {
                    if (lackingPermissions(ctx, "caesar.connections.view")) return;
                    ctx.result(GSON.toJson(LocalStorage.getInstance().getConnections()));
                })
                .delete("/connection", ctx -> {
                    if (lackingPermissions(ctx, "caesar.connections.delete")) return;
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();
                    String connectionName = rootObj.get("name").getAsString();
                    LocalStorage.getInstance().getConnections().removeIf(s -> s.getName().equals(connectionName));
                    LocalStorage.getInstance().saveConnections();
                    ctx.result(createSuccessResponse());
                })

                // User management
                .post("/user", ctx -> {
                    if (lackingPermissions(ctx, "caesar.admin.user.create")) return;
                    if (serviceUnavailable(ctx)) return;
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
                    if (lackingPermissions(ctx, "caesar.admin.user.delete")) return;
                    if (serviceUnavailable(ctx)) return;
                    String username = ctx.pathParam("user");
                    if (StringUtil.isUUID(username)) {
                        UserManager.getInstance().deleteUser(UUID.fromString(username));
                        return;
                    }
                    UserManager.getInstance().deleteUser(username);
                })
                .get("/user/{user}", ctx -> {
                    if (lackingPermissions(ctx, "caesar.admin.user.view")) return;
                    if (serviceUnavailable(ctx)) return;
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
                    if (lackingPermissions(ctx, "caesar.admin.user.edit")) return;
                    if (serviceUnavailable(ctx)) return;
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
                .get("/user", ctx -> ctx.result(GSON.toJson(UserManager.getInstance().getUsers())))
                .patch("/user/password", ctx -> {
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();
                    if (serviceUnavailable(ctx)) return;

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
                    if (lackingPermissions(ctx, "caesar.admin.user.permission.add")) return;
                    if (serviceUnavailable(ctx)) return;
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();

                    String username = rootObj.get("username").getAsString();
                    String permission = rootObj.get("permission").getAsString();
                    UserManager.getInstance().getUser(username).addPermission(permission);
                    StorageFactory.getInstance().getUsedStorage()
                            .updateUser(UserManager.getInstance().getUser(username));
                    ctx.result(createSuccessResponse("Permission added successfully"));
                })
                .post("/user/permissions", ctx -> {
                    if (lackingPermissions(ctx, "caesar.admin.user.permission.add")) return;
                    if (serviceUnavailable(ctx)) return;
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();

                    String username = rootObj.get("username").getAsString();
                    JsonArray permissions = rootObj.get("permissions").getAsJsonArray();
                    User u = UserManager.getInstance().getUser(username);
                    for (JsonElement e : permissions) u.addPermission(e.getAsString());
                    StorageFactory.getInstance().getUsedStorage().updateUser(u);
                    ctx.result(createSuccessResponse("Permissions added successfully"));
                })

                .get("/permission", ctx -> ctx.result(GSON.toJson(UserManager.getInstance().getPermissions())))

                .get("/role", ctx -> {
                    if (lackingPermissions(ctx, "caesar.admin.role.list")) return;
                    if (serviceUnavailable(ctx)) return;
                    ctx.result(GSON.toJson(UserManager.getInstance().getUserRoles()));
                })
                .post("/role", ctx -> {
                    if (lackingPermissions(ctx, "caesar.admin.role.create")) return;
                    if (serviceUnavailable(ctx)) return;
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();
                    String name = rootObj.get("name").getAsString();
                    String color = rootObj.get("color").getAsString();
                    UserRole role = new UserRole(name, color, UUID.randomUUID());
                    UserManager.getInstance().addRole(role);
                    ctx.result(createSuccessResponse());
                })
                .post("/role/permission", ctx -> {
                    if (lackingPermissions(ctx, "caesar.admin.user.permission.add")) return;
                    if (serviceUnavailable(ctx)) return;
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
                    if (lackingPermissions(ctx, "caesar.admin.discord.manage")) return;
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();

                    String token = rootObj.get("token").getAsString();
                    DiscordBot.getInstance().getConfig().setDiscordBotToken(token);
                    LocalStorage.getInstance().saveData();
                    DiscordBot.getInstance().restart();
                    ctx.result(createSuccessResponse());
                })
                .post("/discord/tickets/types", ctx -> {
                    if (lackingPermissions(ctx, "caesar.admin.discord.manage")) return; //TODO: Check if permission exists
                    JsonObject root = JsonParser.parseString(ctx.body()).getAsJsonObject();
                    if (!root.has("name") || !root.has("prefix") || !root.has("showInSel")
                            || !root.has("selText") || !root.has("selEmoji")) {
                        ctx.status(HttpStatus.BAD_REQUEST);
                        ctx.result(createErrorResponse(ErrorType.MISSING_ARGUMENTS));
                        return;
                    }
                    TicketType type = new TicketType(
                            UUID.randomUUID(),
                            root.get("name").getAsString(),
                            root.get("prefix").getAsString(),
                            root.get("showInSel").getAsBoolean(),
                            root.get("selText").getAsString(),
                            root.get("selEmoji").getAsString()
                    );
                    ctx.status(HttpStatus.CREATED);
                    StorageFactory.getInstance().getUsedStorage().addTicketType(type);
                })
                .delete("/discord/tickets/types/{name}", ctx -> {
                    String name = ctx.pathParam("name");
                    if (lackingPermissions(ctx, "caesar.admin.discord.manage")) return; //TODO: Check if permission exists
                    TicketType type = TicketType.valueOf(name);
                    if (type == null) {
                        ctx.status(HttpStatus.BAD_REQUEST);
                        ctx.result(createErrorResponse(ErrorType.DISCORD_TICKET_TYPE_NOT_FOUND));
                        return;
                    }
                    ctx.status(HttpStatus.OK);
                    StorageFactory.getInstance().getUsedStorage().deleteTicketType(type);
                })
                .post("/discord/tickets/status", ctx -> {

                    if (lackingPermissions(ctx, "caesar.admin.discord.manage")) return; //TODO: Check if permission exists
                    JsonObject root = JsonParser.parseString(ctx.body()).getAsJsonObject();
                    if (!root.has("name") || !root.has("description") || !root.has("color")) {
                        ctx.status(HttpStatus.BAD_REQUEST);
                        ctx.result(createErrorResponse(ErrorType.MISSING_ARGUMENTS));
                        return;
                    }
                    TicketStatus status = new TicketStatus(
                            UUID.randomUUID(),
                            root.get("name").getAsString(),
                            root.get("description").getAsString(),
                            DatabaseColorParser.parseColor(root.get("color").getAsString())
                    );
                    StorageFactory.getInstance().getUsedStorage().addTicketStatus(status);
                    ctx.status(HttpStatus.CREATED);
                    ctx.result(createSuccessResponse());
                })
                .delete("/discord/tickets/status", ctx -> {
                    String name = ctx.pathParam("name");
                    if (lackingPermissions(ctx, "caesar.admin.discord.manage")) return; //TODO: Check if permission exists
                    TicketStatus status = TicketStatus.valueOf(name);
                    if (status == null) {
                        ctx.status(HttpStatus.BAD_REQUEST);
                        ctx.result(createErrorResponse(ErrorType.DISCORD_TICKET_STATUS_NOT_FOUND));
                        return;
                    }
                    ctx.status(HttpStatus.OK);
                    StorageFactory.getInstance().getUsedStorage().deleteTicketStatus(status);
                })
                .patch("/discord/tickets/message", ctx -> {

                })
                .delete("/discord/tickets/message", ctx -> {

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
                .get("/config", ctx -> ctx.result(GSON.toJson(LocalStorage.getInstance().getData())))

                // Player management
                .post("/player", ctx -> {
                    if (lackingPermissions(ctx, "caesar.players.create")) return;
                    if (serviceUnavailable(ctx)) return;
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();

                    String id = rootObj.get("playerID").getAsString();
                    try {
                        UUID.fromString(id);
                    } catch (IllegalArgumentException e) {
                        id = UUID.randomUUID().toString();
                    }
                    if (id == null) id = UUID.randomUUID().toString();
                    int number;
                    if (rootObj.has("playerNumber"))
                        number = rootObj.get("playerNumber").getAsInt();
                    else number = new SecureRandom().nextInt(1000, 9999);

                    StorageFactory.getInstance().getUsedStorage().createPlayer(UUID.fromString(id), number);
                    ctx.status(HttpStatus.CREATED);
                    ctx.json(Map.of(
                            "success", true,
                            "playerID", id
                    ));
                })
                .post("/player/mc", ctx -> {
                    if (lackingPermissions(ctx, "caesar.players.edit")) return;
                    if (serviceUnavailable(ctx)) return;
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();

                    UUID playerID = UUID.fromString(rootObj.get("playerID").getAsString());
                    UUID mcID = UUID.fromString(rootObj.get("mcID").getAsString());
                    StorageFactory.getInstance().getUsedStorage().addMCAccount(playerID, mcID);
                    ctx.result(createSuccessResponse());
                })
                .get("/player/id/{id}", ctx -> {
                    if (lackingPermissions(ctx, "caesar.players.view")) return;
                    if (serviceUnavailable(ctx)) return;
                    int number = Integer.parseInt(ctx.pathParam("id"));
                    ctx.result(StorageFactory.getInstance().getUsedStorage().getPlayer(
                            StorageFactory.getInstance().getUsedStorage().getPlayer(number)
                    ).toString());
                })
                .get("/player/uuid/{id}", ctx -> {
                    if (lackingPermissions(ctx, "caesar.players.view")) return;
                    if (serviceUnavailable(ctx)) return;
                    ctx.result(StorageFactory.getInstance().getUsedStorage().getPlayer(
                            UUID.fromString(ctx.pathParam("id"))
                    ).toString());
                })
                .get("/player/mc/name/{name}", ctx -> {
                    if (lackingPermissions(ctx, "caesar.players.view")) return;
                    if (serviceUnavailable(ctx)) return;
                    ctx.result(StorageFactory.getInstance().getUsedStorage().getPlayer(
                            StorageFactory.getInstance().getUsedStorage().getPlayerByAccount(ctx.pathParam("name"))
                    ).toString());
                })
                .delete("/player/{id}", ctx -> {
                    if (lackingPermissions(ctx, "caesar.players.delete")) return;
                    if (serviceUnavailable(ctx)) return;
                    StorageFactory.getInstance().getUsedStorage().deletePlayer(UUID.fromString(ctx.pathParam("id")));
                    ctx.result(createSuccessResponse());
                })
                .post("/player/note", ctx -> {
                    if (lackingPermissions(ctx, "caesar.players.notes.create")) return;
                    if (serviceUnavailable(ctx)) return;
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();
                    UUID playerID = UUID.fromString(rootObj.get("playerID").getAsString());
                    UUID userID = getUserID(ctx);
                    String note = rootObj.get("note").getAsString();
                    StorageFactory.getInstance().getUsedStorage().createPlayerNote(playerID, userID, note);
                    ctx.result(createSuccessResponse());
                })
                .delete("/player/note", ctx -> {
                    if (lackingPermissions(ctx, "caesar.players.notes.delete")) return;
                    if (serviceUnavailable(ctx)) return;
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();
                    UUID noteID = UUID.fromString(rootObj.get("noteID").getAsString());
                    UUID playerID = UUID.fromString(rootObj.get("playerID").getAsString());
                    log.debug(noteID.toString());
                    StorageFactory.getInstance().getUsedStorage().deletePlayerNote(playerID, getUserID(ctx), noteID);
                    ctx.result(createSuccessResponse());
                })

                .get("/support/waiting-room", ctx ->
                        ctx.result(DiscordBot.getInstance().getWaitingRoom().toString()))

                // Process management
                .post("/process", ctx -> {
                    if (lackingPermissions(ctx, "caesar.process.create")) return;
                    if (serviceUnavailable(ctx)) return;
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();
                    UUID type = UUID.fromString(rootObj.get("processType").getAsString());
                    UUID status = UUID.fromString(rootObj.get("processStatus").getAsString());
                    String comment = "Not provided";
                    if (rootObj.has("comment")) comment = rootObj.get("comment").getAsString();
                    UUID processID = StorageFactory.getInstance().getUsedStorage().createProcess(
                            type, status, getUserID(ctx), Optional.of(comment));

                    JsonObject o = new JsonObject();
                    o.addProperty("success", true);
                    o.addProperty("processID", processID.toString());
                    ctx.result(o.toString());
                })
                .patch("/process/status", ctx -> {
                    if (lackingPermissions(ctx, "caesar.process.change-status")) return;
                    if (serviceUnavailable(ctx)) return;
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();
                    UUID processID = UUID.fromString(rootObj.get("processID").getAsString());
                    UUID processStatus = UUID.fromString(rootObj.get("processStatus").getAsString());
                    StorageFactory.getInstance().getUsedStorage().updateProcessStatus(processID, processStatus);
                    ctx.result(createSuccessResponse());
                })
                .patch("/process/player", ctx -> {
                    if (lackingPermissions(ctx, "caesar.process.assign-player")) return;
                    if (serviceUnavailable(ctx)) return;
                    JsonObject rootObj = JsonParser.parseString(ctx.body()).getAsJsonObject();
                    UUID processID = UUID.fromString(rootObj.get("processID").getAsString());
                    UUID playerID = UUID.fromString(rootObj.get("playerID").getAsString());
                    StorageFactory.getInstance().getUsedStorage().assignPlayerToProcess(processID, playerID);
                    ctx.result(createSuccessResponse());
                })

                // Dashboard Statistics
                .get("/dashboard/support", ctx -> {

                })
                .get("/dashboard/server", ctx -> {

                })
                .get("/dashboard/discord", ctx -> {
                    JsonObject o = new JsonObject();
                    o.addProperty("success", true);
                    o.addProperty("route", "discord");
                    o.addProperty("provider", "caesar");
                    o.addProperty("members", DiscordBot.getInstance().getMainGuild().getMemberCount());
                    o.addProperty("online", DiscordBot.getInstance().getOnlineMembers());
                    o.addProperty("bots", DiscordBot.getInstance().getBotMembers());
                    ctx.result(o.toString());

                })
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


    private boolean lackingPermissions(Context ctx, String... requiredPermissions) {
        String token = ctx.header("Authorization");
        if (token == null) {
            ctx.skipRemainingHandlers().result(createErrorResponse(ErrorType.NO_PERMISSION)).status(HttpStatus.FORBIDDEN);
            return true;
        }
        token = token.replace("Bearer ", "");
        DecodedJWT decoded = JWTUtil.getInstance().decode(token);
        User user = UserManager.getInstance().getUser(decoded.getSubject());
        if (user.getUsername().equals("admin")) return false;
        for (String requiredPermission : requiredPermissions) {
            if (!user.getPermissions().contains(requiredPermission)) {
                ctx.skipRemainingHandlers().result(createErrorResponse(ErrorType.NO_PERMISSION)).status(HttpStatus.FORBIDDEN);
                return true;
            }
        }
        return false;
    }

    private boolean serviceUnavailable(Context ctx) {
        if (StorageFactory.getInstance().getUsedStorage() == null) {
            ctx.status(HttpStatus.SERVICE_UNAVAILABLE);
            ctx.header("Cache-Control", "no-store");
            ctx.header("Retry-After", "3600");
            return true;
        }
        return false;
    }

    private UUID getUserID(Context ctx) {
        String token = ctx.header("Authorization");
        if (token == null) return null;
        token = token.replace("Bearer ", "");
        DecodedJWT decoded = JWTUtil.getInstance().decode(token);
        User user = UserManager.getInstance().getUser(decoded.getSubject());
        return user.getUuid();
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
        INVALID_SETUP_CODE,
        NO_PERMISSION,
        MISSING_ARGUMENTS,
        DISCORD_TICKET_TYPE_NOT_FOUND,
        DISCORD_TICKET_STATUS_NOT_FOUND
    }
}