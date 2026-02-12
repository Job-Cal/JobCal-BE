package kr.co.jobcal.global.oauth;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.util.StringUtils;

public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver delegate;

    public CustomAuthorizationRequestResolver(
        ClientRegistrationRepository clientRegistrationRepository,
        String authorizationRequestBaseUri
    ) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
            clientRegistrationRepository,
            authorizationRequestBaseUri
        );
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authRequest = delegate.resolve(request);
        return customizeRedirectUriIfNeeded(request, authRequest);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authRequest = delegate.resolve(request, clientRegistrationId);
        return customizeRedirectUriIfNeeded(request, authRequest);
    }

    private OAuth2AuthorizationRequest customizeRedirectUriIfNeeded(
        HttpServletRequest request,
        OAuth2AuthorizationRequest authRequest
    ) {
        if (authRequest == null) {
            return null;
        }

        String mode = request.getParameter("mode");
        String origin = request.getHeader("Origin");
        boolean hasMode = StringUtils.hasText(mode);
        Map<String, Object> additionalParameters = new LinkedHashMap<>(authRequest.getAdditionalParameters());
        if (hasMode) {
            additionalParameters.put("mode", mode);
        }
        if (!isLocalRequest(mode, origin)) {
            if (!hasMode) {
                return authRequest;
            }
            return OAuth2AuthorizationRequest.from(authRequest)
                .additionalParameters(additionalParameters)
                .build();
        }

        String localRedirectUri = "http://localhost:8080/api/login/oauth2/code/cognito";
        return OAuth2AuthorizationRequest.from(authRequest)
            .redirectUri(localRedirectUri)
            .additionalParameters(withRedirectUri(additionalParameters, localRedirectUri))
            .build();
    }

    private Map<String, Object> withRedirectUri(
        Map<String, Object> additionalParameters,
        String redirectUri
    ) {
        Map<String, Object> updated = new LinkedHashMap<>(additionalParameters);
        updated.put("redirect_uri", redirectUri);
        return updated;
    }

    private boolean isLocalRequest(String mode, String origin) {
        if ("local".equalsIgnoreCase(mode)) {
            return true;
        }
        if (!StringUtils.hasText(origin)) {
            return false;
        }
        try {
            URI uri = URI.create(origin);
            String host = uri.getHost();
            return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
