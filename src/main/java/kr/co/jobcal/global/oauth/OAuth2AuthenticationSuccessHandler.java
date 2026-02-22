package kr.co.jobcal.global.oauth;

import java.io.IOException;
import kr.co.jobcal.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);
    private final UserService userService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final String frontendUrl;
    private final CookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    public OAuth2AuthenticationSuccessHandler(
        UserService userService,
        OAuth2AuthorizedClientService authorizedClientService,
        @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl,
        CookieOAuth2AuthorizationRequestRepository authorizationRequestRepository
    ) {
        this.userService = userService;
        this.authorizedClientService = authorizedClientService;
        this.frontendUrl = frontendUrl;
        this.authorizationRequestRepository = authorizationRequestRepository;
    }

    @Override
    public void onAuthenticationSuccess(
        jakarta.servlet.http.HttpServletRequest request,
        jakarta.servlet.http.HttpServletResponse response,
        Authentication authentication
    ) throws IOException {
        userService.upsertFromClaims(AuthClaimsExtractor.extractClaims(authentication));

        authorizationRequestRepository.removeAuthorizationRequest(request, response);

        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName()
            );
            if (client != null && client.getAccessToken() != null) {
                if (client.getRefreshToken() != null) {
                    setHttpOnlyCookie(response, "refreshToken", client.getRefreshToken().getTokenValue());
                }
            }
        }

        String redirectUrl = frontendUrl.contains("?")
            ? frontendUrl + "&justLoggedIn=1"
            : frontendUrl + "?justLoggedIn=1";
        log.info("OAuth2 login redirect -> {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    private void setHttpOnlyCookie(jakarta.servlet.http.HttpServletResponse response, String name, String value) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(true)
            .sameSite("None")
            .path("/")
            .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
