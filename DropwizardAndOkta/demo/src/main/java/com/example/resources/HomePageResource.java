package com.example.resources;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

@Path("/")
public class HomePageResource {

    LoginResource login;

    public HomePageResource(LoginResource login){
        this.login = login;
    }

    @GET
    public String handleGetRequest(@Context HttpServletRequest request) throws Exception{

        login.authenticate(request);

        return "Hello " + request.getSession().getAttribute("name") +
                "! We'll be contacting you at: " + request.getSession().getAttribute("email");
    }

}
