package com.example.resources;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.*;

import com.example.DemoConfiguration;
import com.okta.jwt.Jwt;
import com.okta.jwt.JwtVerifier;
import com.okta.jwt.JwtHelper;
import org.glassfish.jersey.client.JerseyClientBuilder;


@Path("/login")
public class LoginResource {

    public static class JWT {
        public String access_token;
        public String token_type;
        public String expires_in;
        public String scope;
        public String id_token;
    }

    DemoConfiguration config;

    public LoginResource(DemoConfiguration configuration){

        config = configuration;
    }

    public void authenticate(HttpServletRequest request) throws Exception {

        HttpSession session = request.getSession(false);

        if(session == null || session.getAttribute("email") == null || session.getAttribute("name") == null) {
            throw new RedirectionException(302, new URI(config.issuer + "/v1/authorize?" +
                    "response_type=code" +
                    "&client_id=" + config.clientId +
                    "&redirect_uri=" + config.callbackUri +
                    "&scope=openid+profile+email" +
                    "&state=" + request.getRequestURI()+
                    "&nonce=foo"));
        }
    }

    @GET
    public Response handleLogin(@Context HttpServletRequest request) throws Exception {

        JWT jwt =  exchangeAuthCodeForToken(request);

        addIdClaimsToSession(request, jwt);

        URI originallyRequestedUri = new URI(request.getParameter("state"));

        return Response.status(Response.Status.FOUND).location(originallyRequestedUri).build();
    }


    private JWT exchangeAuthCodeForToken(HttpServletRequest request){

        Client client = new JerseyClientBuilder().build();
        WebTarget webTarget = client.target(config.issuer+"/v1/token");

        Invocation.Builder invocationBuilder =  webTarget.request(MediaType.APPLICATION_JSON);

        MultivaluedMap formData = new MultivaluedHashMap();

        formData.put("grant_type", Arrays.asList("authorization_code"));
        formData.put("code", Arrays.asList(request.getParameter("code")));
        formData.put("redirect_uri",Arrays.asList(config.callbackUri));
        formData.put("client_id",Arrays.asList(config.clientId));
        formData.put("client_secret",Arrays.asList(config.clientSecret));
        formData.put("scope",Arrays.asList("openid profile email"));

        Response response = invocationBuilder.post(Entity.entity(formData, MediaType.APPLICATION_FORM_URLENCODED));

        JWT jwt = response.readEntity(JWT.class);

        return jwt;
    }


    private void addIdClaimsToSession(HttpServletRequest request, JWT jwt) throws Exception {

        JwtVerifier jwtVerifier = new JwtHelper()
                .setIssuerUrl(config.issuer)
                .setAudience("api://default")
                .setClientId(config.clientId)
                .build();

        Jwt idTokenJwt = jwtVerifier.decodeIdToken(jwt.id_token, null);

        Map<String, Object> claims = idTokenJwt.getClaims();
        HttpSession session = request.getSession(true);

        session.setAttribute("name", claims.get("name"));
        session.setAttribute("email", claims.get("email"));
    }

}
