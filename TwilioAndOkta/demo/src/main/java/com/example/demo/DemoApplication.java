package com.example.demo;

import com.okta.sdk.authc.credentials.TokenClientCredentials;
import com.okta.sdk.client.Clients;
import com.okta.sdk.resource.user.User;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;

@RestController
@EnableOAuth2Sso
@SpringBootApplication
public class DemoApplication {

    // Twilio Account SID from www.twilio.com/user/account
    static final String twilioAccountSid = "{twilio-account-SID}";

    // Twilio Auth Token from www.twilio.com/user/account
    static final String twilioAuthToken = "{twilio-auth-token}";

    // Okta Token from the API -> Tokens area of your Okta dev dashboard
    static final String oktaToken = "{okta-token}";

    // Your personal Okta URL
    static final String oktaUrl = "https://dev-{okta-ID}.oktapreview.com";

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
        Twilio.init(twilioAccountSid, twilioAuthToken);
    }


    private boolean isNewLocation(HttpServletRequest request, User user) {

        String currentIP = request.getRemoteAddr();

        ArrayList<String> locations = (ArrayList<String>) user.getProfile().get("signInLocations");

        if(locations.contains(currentIP)){
            return false;
        }

        //otherwise, this is a new IP.  let's store it in Okta!
        locations.add(currentIP);
        user.update();
        return true;
    }

    @GetMapping("/")
    public String sendATextMessage(HttpServletRequest request, Principal principal) {

        User user = Clients.builder()
                            .setClientCredentials(new TokenClientCredentials(oktaToken))
                            .setOrgUrl(oktaUrl)
                            .build()
                            .getUser(principal.getName());

        String userPhoneNumber = user.getProfile().getMobilePhone();
        String userFirstName = user.getProfile().getFirstName();

        boolean newLocation = isNewLocation(request, user);

        if(newLocation && userPhoneNumber != null) {

            String messageBody = "Hello " +
                                    userFirstName +
                                    ", a new sign-in has been detected at " +
                                    new Date() +
                                    " from IP address " +
                                    request.getRemoteAddr();

            Message.creator(new PhoneNumber(userPhoneNumber), // To number
                            new PhoneNumber("+15005550006"),  // From number
                            messageBody // SMS body
                            ).create();
        }

        return "Hello " + userFirstName + "!";
    }
}

