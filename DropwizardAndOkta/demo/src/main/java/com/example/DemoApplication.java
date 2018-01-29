package com.example;

import com.example.resources.LoginResource;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import com.example.resources.HomePageResource;
import org.eclipse.jetty.server.session.SessionHandler;


public class DemoApplication extends Application<DemoConfiguration> {

    public static void main(final String[] args) throws Exception {
        new DemoApplication().run(args);
    }

    @Override
    public String getName() {
        return "Demo";
    }

    @Override
    public void initialize(final Bootstrap<DemoConfiguration> bootstrap) { }

    @Override
    public void run(final DemoConfiguration configuration,
                    final Environment environment) {

        LoginResource login = new LoginResource(configuration);
        environment.jersey().register(login);
        environment.jersey().register(new HomePageResource(login));
        environment.servlets().setSessionHandler(new SessionHandler());
    }
}
