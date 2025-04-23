package de.julianweinelt.caesar.core.webserver;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.julianweinelt.caesar.core.authentication.JWTUtil;
import de.julianweinelt.caesar.core.authentication.UserManager;
import de.julianweinelt.caesar.core.configuration.ConfigurationManager;
import de.julianweinelt.caesar.core.util.logging.Log;
import de.julianweinelt.caesar.core.util.ui.DisplayComponent;
import de.julianweinelt.caesar.core.util.ui.DisplayComponentType;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Base64;

public class WebServer {
    private final JWTUtil jwtUtil = new JWTUtil();
    private final Gson gson = new Gson();

    public void start() {
        Javalin app = Javalin.create()
                .before(context -> {
                    context.contentType("application/json");
                })
                .post("/checkconnection", context -> {
                    JsonObject o = new JsonObject();
                    o.addProperty("success", true);
                    context.result(o.toString());
                })
                .post("/auth", context -> {
                    String authBasic = context.header("Authorization");
                    if (authBasic == null || !authBasic.startsWith("Basic ")) {
                        context.status(401);
                        context.result(createErrorResponse("Unauthorized"));
                        context.skipRemainingHandlers();
                        return;
                    }
                    String base64 = authBasic.substring(6);

                    byte[] base64DecodedBytes = Base64.getDecoder().decode(base64);
                    String decodedString = new String(base64DecodedBytes);

                    String user = decodedString.split(":")[0];
                    String pass = decodedString.split(":")[1];

                    if (UserManager.getInstance().verify(user, pass)) {
                        JsonObject o = new JsonObject();
                        o.addProperty("success", true);
                        o.addProperty("token", jwtUtil.token(user));
                        context.result(o.toString());
                    } else {
                        context.status(401);
                        context.result(createErrorResponse("Invalid username or password"));
                        context.skipRemainingHandlers();
                    }
                })
                .before(context -> {
                    String token = context.header("Authorization");
                    if (token == null || !token.startsWith("Bearer ")) {
                        context.status(401);
                        context.result(createErrorResponse("Unauthorized"));
                        context.skipRemainingHandlers();
                        return;
                    }
                    token = token.replace("Bearer ", "");
                    DecodedJWT decodedJWT = jwtUtil.verify(token);
                    if (decodedJWT == null) {
                        context.status(401);
                        context.result(createErrorResponse("Invalid token"));
                        context.skipRemainingHandlers();
                        return;
                    }

                    if (UserManager.getInstance().getUser(decodedJWT.getSubject()) == null) {
                        context.status(401);
                        context.result(createErrorResponse("Invalid user"));
                        context.skipRemainingHandlers();
                    }
                })
                .post("/users/changepassword", context -> {
                    String token = context.header("Authorization").replace("Bearer ", "");

                    JsonObject rootObj = JsonParser.parseString(context.body()).getAsJsonObject();
                    String username = jwtUtil.extractUsername(token);
                    String password = rootObj.get("password").getAsString();
                    UserManager.getInstance().getUser(username).setPasswordHashed(password.hashCode() + "");
                    context.status(200);
                    context.result(createOKResponse());
                })
                .post("/users/create", context -> {
                    JsonObject rootObj = JsonParser.parseString(context.body()).getAsJsonObject();
                    String username = rootObj.get("password").getAsString();
                    String password = rootObj.get("password").getAsString();
                    UserManager.getInstance().createUser(username, password);
                    context.status(200);
                    context.result(createOKResponse());
                })
                .post("/users/delete", context -> {
                    JsonObject rootObj = JsonParser.parseString(context.body()).getAsJsonObject();
                    String username = rootObj.get("username").getAsString();
                })
                .post("/users/setactive", context -> {
                    JsonObject rootObj = JsonParser.parseString(context.body()).getAsJsonObject();
                    String username = rootObj.get("username").getAsString();
                    boolean isActive = rootObj.get("isActive").getAsBoolean();
                    UserManager.getInstance().getUser(username).setActive(isActive);
                })
                .get("/users/permissions", context -> {
                    String token = context.header("Authorization").replace("Bearer ", "");
                    String username = jwtUtil.extractUsername(token);
                    context.result(gson.toJson(UserManager.getInstance().getUser(username).getPermissions()));
                    context.status(200);
                })
                .post("/users/addpermission", context -> {
                    JsonObject rootObj = JsonParser.parseString(context.body()).getAsJsonObject();
                    String username = rootObj.get("username").getAsString();
                    String[] permissions = rootObj.get("permissions").getAsString().split(",");
                    UserManager.getInstance().addUserPermissions(permissions, username);
                    context.status(200);
                    context.result(createOKResponse());
                })
                .post("/setup/cloudnetsetup", context -> {
                    JsonObject rootObj = JsonParser.parseString(context.body()).getAsJsonObject();
                    String cloudUsername = rootObj.get("username").getAsString();
                    String cloudPassword = rootObj.get("password").getAsString();
                    ConfigurationManager.getInstance().getConfig().addComponent(
                            DisplayComponent.Builder.create(DisplayComponentType.STRING, "cloud.username",
                                    cloudUsername, false)).addComponent(
                                            DisplayComponent.Builder.create(
                                                    DisplayComponentType.STRING, "cloud.password", cloudPassword, false
                                            )
                    );
                    ConfigurationManager.getInstance().save();
                })
                .get("/files/path", context -> {
                    String path = context.header("Path");
                    if (path == null || path.isEmpty()) {
                        context.status(HttpStatus.BAD_REQUEST);
                        context.result(createErrorResponse("Invalid path"));
                        context.skipRemainingHandlers();
                        return;
                    }
                    FTPResponse response = new FTPResponse();
                    response.setup(new File(path));
                    context.result(response.toString());
                })
                .get("/files/file", context -> {
                    String path = context.header("Path");

                    File file = new File(path);
                    if (!file.exists()) {
                        context.status(HttpStatus.BAD_REQUEST);
                        context.result(createErrorResponse("Invalid path"));
                        context.skipRemainingHandlers();
                        return;
                    }
                    if (!file.isFile()) {
                        context.status(HttpStatus.BAD_REQUEST);
                        context.result(createErrorResponse("Invalid path"));
                        context.skipRemainingHandlers();
                        return;
                    }
                    StringBuilder result = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            result.append(line);
                        }
                    } catch (Exception e) {
                        context.status(HttpStatus.BAD_REQUEST);
                        context.result(createErrorResponse("Invalid path"));
                        context.skipRemainingHandlers();
                        Log.error(e.getMessage());
                    }

                    context.result(result.toString());
                    context.status(200);
                })
                .post("/files/upload", context -> {

                })

                .get("/permissions", context -> {
                    String username = context.header("username");
                    if (username == null || username.isEmpty()) {
                        context.status(HttpStatus.BAD_REQUEST);
                        context.result(createErrorResponse("Invalid username"));
                        context.skipRemainingHandlers();
                        return;
                    }
                    if (UserManager.getInstance().getUser(username) == null) {
                        context.status(HttpStatus.BAD_REQUEST);
                        context.result(createErrorResponse("Invalid username"));
                        context.skipRemainingHandlers();
                        return;
                    }
                    context.result(
                            gson.toJson(UserManager.getInstance().getUser(username).getPermissions())
                    );
                    context.status(200);
                })
                .start(6565);
    }


    public String createOKResponse(String message) {
        JsonObject o = new JsonObject();
        o.addProperty("success", true);
        o.addProperty("message", message);
        return o.toString();
    }

    public String createOKResponse() {
        JsonObject o = new JsonObject();
        o.addProperty("success", true);
        o.addProperty("message", "Not provided");
        return o.toString();
    }

    public String createErrorResponse(String message) {
        JsonObject o = new JsonObject();
        o.addProperty("success", false);
        o.addProperty("message", message);
        return o.toString();
    }
}
