package kr.co.jobcal.security;

import java.util.Collections;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class AuthClaimsExtractor {

    private AuthClaimsExtractor() {}

    public static Map<String, Object> extractClaims(Authentication authentication) {
        if (authentication == null) {
            return Collections.emptyMap();
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return jwt.getClaims();
        }

        if (authentication instanceof OAuth2AuthenticationToken oauth2Auth) {
            if (oauth2Auth.getPrincipal() instanceof OidcUser oidcUser) {
                return oidcUser.getClaims();
            }
            return oauth2Auth.getPrincipal().getAttributes();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            return oidcUser.getClaims();
        }

        return Collections.emptyMap();
    }
}
