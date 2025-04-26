package de.julianweinelt.caesar.endpoint;

import com.google.gson.JsonObject;
import io.javalin.Javalin;

import java.util.Base64;

public class CaesarServer {
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
                    o.addProperty("token", JWTUtil.mailToken(mailUser, m.getMailID().toString()));
                    ctx.result(o.toString());
                })
                .start(6565);
    }
}
