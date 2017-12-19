package com.example.demo;


import com.okta.jwt.Jwt;
import com.okta.jwt.JwtHelper;
import com.okta.jwt.JwtVerifier;
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

    final static String OKTA_CLIENT_ID = "0oacqisv6qsvBoshD0h7";
    final static String OKTA_CLIENT_SECRET = "v9DG9LiG96wzCSL-xvOsFLvUbcegVxW_HL2G-z6k";
    final static String OKTA_DEV_DOMAIN = "https://dev-279161.oktapreview.com/oauth2/default";
    final static String OKTA_CALLBACK_URL = "http://localhost:8080/login";

    AuthHandler getOAuthHandler(Router router){
        OAuth2Auth oauth2 = OAuth2Auth.create(vertx, OAuth2FlowType.AUTH_CODE, new OAuth2ClientOptions()
            .setClientID(OKTA_CLIENT_ID)
            .setClientSecret(OKTA_CLIENT_SECRET)
            .setSite(OKTA_DEV_DOMAIN)
            .setTokenPath("/v1/token")
            .setAuthorizationPath("/v1/authorize")
            .setUserInfoPath("/v1/userinfo")
            .setUseBasicAuthorizationHeader(false)
        );

        OAuth2AuthHandler authHandler = OAuth2AuthHandler.create(oauth2, OKTA_CALLBACK_URL);
        authHandler.extraParams(new JsonObject("{\"scope\":\"openid profile email\"}"));
        authHandler.setupCallback(router.route());

        return authHandler;
    }

    Map<String, Object> getIdClaims(RoutingContext ctx) {
        try {
            JwtVerifier jwtVerifier = new JwtHelper()
                .setIssuerUrl(OKTA_DEV_DOMAIN)
                .setAudience("api://default")
                .setClientId(OKTA_CLIENT_ID)
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

        vertx.createHttpServer().requestHandler(router::accept).listen(8080);
    }
}
