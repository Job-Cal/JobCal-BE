package kr.co.jobcal.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final OAuth2AuthorizedClientService authorizedClientService;

    public AuthController(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    @GetMapping("/token")
    public void issueAccessToken(Authentication authentication, HttpServletResponse response) {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
            oauthToken.getAuthorizedClientRegistrationId(),
            oauthToken.getName()
        );

        if (client == null || client.getAccessToken() == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        response.setHeader("Authorization", "Bearer " + client.getAccessToken().getTokenValue());
        response.setStatus(HttpStatus.OK.value());
    }
}
