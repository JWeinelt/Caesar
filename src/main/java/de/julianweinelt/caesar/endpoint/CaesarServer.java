package de.julianweinelt.caesar.endpoint;

import io.javalin.Javalin;

public class CaesarServer {
    public void start() {
        Javalin app = Javalin.create()
                .before(ctx -> ctx.contentType("application/json"))
                .post("/auth", ctx -> {

                })
                .start(6565);
    }
}
