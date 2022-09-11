package com.vkashel

import io.helidon.common.configurable.Resource
import io.helidon.common.http.Http
import io.helidon.config.Config
import io.helidon.security.Role
import io.helidon.security.Security
import io.helidon.security.SecurityContext
import io.helidon.security.integration.webserver.WebSecurity
import io.helidon.security.jwt.Jwt
import io.helidon.security.jwt.SignedJwt
import io.helidon.security.jwt.jwk.JwkKeys
import io.helidon.security.providers.jwt.JwtProvider
import io.helidon.webserver.Routing
import io.helidon.webserver.WebServer
import java.net.URI
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant

fun main() {
    val path =
        Paths.get(object {}.javaClass.classLoader.getResource("jwks.json")?.toURI() ?: URI(""))
    val jwtKeys = JwkKeys.builder().resource(Resource.create(path)).build()
    val provider = JwtProvider.builder()
        .verifyJwk(Resource.create(path))
    val security = Security.builder()
        .addProvider(provider)
        .build()
    val jwtBuilder = Jwt.builder().keyId("jVVHiNPRVnx-qZEXGaQdgllOP2w6G4dgIRJlGXNvq0g")
        .subject("subject")
        .notBefore(Instant.now())
        .expirationTime(Instant.now().plus(Duration.ofHours(2)))
        .addUserGroup("ADMIN")
        .addUserGroup("TEST")
    val routing = Routing.builder()
        .register(WebSecurity.create(security))
        .get("/token", { rq, rs ->
            val signed = SignedJwt.sign(jwtBuilder.build(), jwtKeys)
            rs.status(Http.Status.OK_200).send(signed.tokenContent())
        })
        .get("/profile", WebSecurity.authenticate(), { rq, rs ->
            val context = rq.context().get(SecurityContext::class.java).orElse(null)
            if (context == null) {
                rs.status(Http.Status.INTERNAL_SERVER_ERROR_500).send()
            } else {
                val authentication = context.atnClientBuilder().buildAndGet()
                val user = authentication.user().get()
                val response = mutableMapOf<String, Any>()
                response["subject"] = user.principal().name
                response["roles"] = user.grants(Role::class.java)
                rs.status(Http.Status.OK_200).send(response.toString())
            }
        })
        .build()

    val config = Config.create()
    val server = WebServer.builder(routing)
        .config(config.get("server"))
        .build()
        .start()
    println("Application started at ${server.get().port()} port")
}