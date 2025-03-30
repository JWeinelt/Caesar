package de.julianweinelt.caesar.core.webserver;

import io.javalin.Javalin;

public class WebServer {
    public void start() {
        Javalin app = Javalin.create()
                .post("/auth", context -> {

                })
                .start(6565);
    }
}
