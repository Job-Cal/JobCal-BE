package kr.co.jobcal.security;

import java.io.IOException;
import kr.co.jobcal.service.UserService;
import org.springframework.http.ResponseCookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final String frontendUrl;

    public OAuth2AuthenticationSuccessHandler(
        UserService userService,
        OAuth2AuthorizedClientService authorizedClientService,
        @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl
    ) {
        this.userService = userService;
        this.authorizedClientService = authorizedClientService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(
        jakarta.servlet.http.HttpServletRequest request,
        jakarta.servlet.http.HttpServletResponse response,
        Authentication authentication
    ) throws IOException {
        userService.upsertFromClaims(AuthClaimsExtractor.extractClaims(authentication));

        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName()
            );
            if (client != null && client.getAccessToken() != null) {
                response.setHeader("Authorization", "Bearer " + client.getAccessToken().getTokenValue());
            }
            if (client != null && client.getRefreshToken() != null) {
                ResponseCookie cookie = ResponseCookie.from("refresh_token", client.getRefreshToken().getTokenValue())
                    .httpOnly(true)
                    .secure(request.isSecure())
                    .sameSite("Lax")
                    .path("/")
                    .build();
                response.addHeader("Set-Cookie", cookie.toString());
            }
        }

        response.sendRedirect(frontendUrl);
    }
}
