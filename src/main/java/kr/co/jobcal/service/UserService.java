package kr.co.jobcal.service;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import kr.co.jobcal.entity.User;
import kr.co.jobcal.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User upsertFromClaims(Map<String, Object> claims) {
        String email = extractEmail(claims);
        String userId = extractUserId(claims, email);

        return userRepository.findByEmail(email)
            .map(existing -> {
                if (userId != null && !Objects.equals(existing.getUserId(), userId)) {
                    existing.setUserId(ensureUniqueUserId(userId));
                }
                return existing;
            })
            .orElseGet(() -> {
                User user = new User();
                user.setEmail(email);
                user.setUserId(ensureUniqueUserId(userId));
                user.setHashedPassword("oauth2:" + UUID.randomUUID());
                user.setIsActive(true);
                return userRepository.save(user);
            });
    }

    private String extractEmail(Map<String, Object> claims) {
        String email = claimAsString(claims, "email");
        if (email != null && !email.isBlank()) {
            return email;
        }

        String username = claimAsString(claims, "username");
        if (username != null && username.contains("@")) {
            return username;
        }

        String cognitoUsername = claimAsString(claims, "cognito:username");
        if (cognitoUsername != null && cognitoUsername.contains("@")) {
            return cognitoUsername;
        }

        String sub = claimAsString(claims, "sub");
        if (sub != null) {
            return sub + "@cognito.local";
        }

        throw new IllegalArgumentException("Missing email claim from authentication token");
    }

    private String extractUserId(Map<String, Object> claims, String fallbackEmail) {
        String userId = claimAsString(claims, "preferred_username");
        if (userId == null) {
            userId = claimAsString(claims, "username");
        }
        if (userId == null) {
            userId = claimAsString(claims, "cognito:username");
        }
        if (userId == null) {
            userId = fallbackEmail != null ? fallbackEmail.split("@")[0] : null;
        }
        return sanitizeUserId(userId);
    }

    private String ensureUniqueUserId(String base) {
        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUserId(candidate)) {
            candidate = base + "_" + suffix;
            suffix += 1;
        }
        return candidate;
    }

    private String sanitizeUserId(String input) {
        if (input == null || input.isBlank()) {
            return "user";
        }
        String normalized = input.toLowerCase().replaceAll("[^a-z0-9._-]", "");
        if (normalized.isBlank()) {
            return "user";
        }
        return normalized;
    }

    private String claimAsString(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }
}
