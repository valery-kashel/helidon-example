package com.vkashel

import io.helidon.config.Config
import io.helidon.webserver.Routing
import io.helidon.webserver.WebServer


fun main() {
    val config = Config.create()
    val routing = Routing.builder()
        .get("/hello", { _, res -> res.send("Hello world!") })

    WebServer.create(routing, config.get("server"))
        .start()
}