package kr.co.jobcal.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RestClient restClient;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final String fallbackTokenUri;
    private final String fallbackClientId;
    private final String fallbackClientSecret;

    public AuthController(
        ClientRegistrationRepository clientRegistrationRepository,
        @Value("${spring.security.oauth2.client.provider.cognito.token-uri:}") String fallbackTokenUri,
        @Value("${spring.security.oauth2.client.registration.cognito.client-id:}") String fallbackClientId,
        @Value("${spring.security.oauth2.client.registration.cognito.client-secret:}") String fallbackClientSecret
    ) {
        this.restClient = RestClient.builder().build();
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.fallbackTokenUri = fallbackTokenUri;
        this.fallbackClientId = fallbackClientId;
        this.fallbackClientSecret = fallbackClientSecret;
    }

    @GetMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshAccessToken(
        @CookieValue(value = "refreshToken", required = false) String refreshToken,
        HttpServletResponse response
    ) {
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("cognito");
        String tokenUri = registration != null
            ? registration.getProviderDetails().getTokenUri()
            : fallbackTokenUri;
        String clientId = registration != null ? registration.getClientId() : fallbackClientId;
        String clientSecret = registration != null ? registration.getClientSecret() : fallbackClientSecret;

        if (!StringUtils.hasText(tokenUri) || !StringUtils.hasText(clientId)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);
        form.add("client_id", clientId);
        if (clientSecret != null && !clientSecret.isBlank()) {
            form.add("client_secret", clientSecret);
        }

        try {
            Map<String, Object> tokenResponse = restClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

            if (tokenResponse == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Object accessTokenValue = tokenResponse.get("access_token");
            if (!(accessTokenValue instanceof String accessToken) || accessToken.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

            Object nextRefreshTokenValue = tokenResponse.get("refresh_token");
            if (nextRefreshTokenValue instanceof String nextRefreshToken && !nextRefreshToken.isBlank()) {
                setCookie(response, "refreshToken", nextRefreshToken);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("accessToken", accessToken);
            body.put("expiresIn", tokenResponse.get("expires_in"));
            body.put("tokenType", tokenResponse.get("token_type"));
            return ResponseEntity.ok(body);
        } catch (RestClientResponseException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    private void setCookie(HttpServletResponse response, String name, String value) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(true)
            .sameSite("None")
            .path("/")
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
