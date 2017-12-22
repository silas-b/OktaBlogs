package com.example.demo;


import com.okta.jwt.Jwt;
import com.okta.jwt.JwtHelper;
import com.okta.jwt.JwtVerifier;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthHandler;
import io.vertx.ext.web.handler.OAuth2AuthHandler;

import java.util.HashMap;
import java.util.Map;

public class MainVerticle extends AbstractVerticle {

    AuthHandler getOAuthHandler(Router router){
        OAuth2Auth oauth2 = OAuth2Auth.create(vertx, OAuth2FlowType.AUTH_CODE, new OAuth2ClientOptions()
                .setClientID(config().getString("clientId"))
                .setClientSecret(config().getString("clientSecret"))
                .setSite(config().getString("issuer"))
                .setTokenPath("/v1/token")
                .setAuthorizationPath("/v1/authorize")
                .setUserInfoPath("/v1/userinfo")
                .setUseBasicAuthorizationHeader(false)
        );

        OAuth2AuthHandler authHandler = OAuth2AuthHandler.create(oauth2, config().getString("callbackUrl"));
        authHandler.extraParams(new JsonObject("{\"scope\":\"openid profile email\"}"));
        authHandler.setupCallback(router.route());

        return authHandler;
    }

    Map<String, Object> getIdClaims(RoutingContext ctx) {
        try {
            JwtVerifier jwtVerifier = new JwtHelper()
                    .setIssuerUrl(config().getString("issuer"))
                    .setAudience("api://default")
                    .setClientId(config().getString("clientId"))
                    .build();

            Jwt idTokenJwt = jwtVerifier.decodeIdToken(ctx.user().principal().getString("id_token"), null);
            return idTokenJwt.getClaims();
        }catch(Exception e){
            //do something with the exception...
            return new HashMap<>();
        }
    }

    @Override
    public void start() throws Exception {

        ConfigStoreOptions fileStore = new ConfigStoreOptions()
                .setType("file")
                .setConfig(new JsonObject().put("path", "src/main/application.json"));

        ConfigRetrieverOptions options = new ConfigRetrieverOptions()
                .addStore(fileStore);

        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);

        retriever.getConfig(ar -> {
            if (ar.failed()) {

                System.err.println("failed to retrieve config.");
            } else {

                config().mergeIn(ar.result());
                startServer();
            }
        });
    }


    void startServer(){

        Router router = Router.router(vertx);
        AuthHandler authHandler = getOAuthHandler(router);

        router.route("/private/*").handler(authHandler);

        router.route("/private/secret").handler(ctx -> {

            Map claims = getIdClaims(ctx);

            ctx.response().end("Hi " +
                    claims.get("name") +
                    ", the email address we have on file for you is: "+
                    claims.get("email"));
        });

        int port = config().getInteger("port");
        vertx.createHttpServer().requestHandler(router::accept).listen(port);
    }
}
