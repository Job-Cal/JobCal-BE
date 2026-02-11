package kr.co.jobcal.service;

import java.util.Map;
import kr.co.jobcal.entity.User;
import kr.co.jobcal.security.AuthClaimsExtractor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    private final UserService userService;

    public CurrentUserProvider(UserService userService) {
        this.userService = userService;
    }

    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("Unauthenticated request");
        }

        Map<String, Object> claims = AuthClaimsExtractor.extractClaims(authentication);
        User user = userService.upsertFromClaims(claims);
        return user.getUserId();
    }
}
