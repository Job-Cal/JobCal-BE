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
        String providerSubject = extractProviderSubject(claims);
        String email = extractEmail(claims, providerSubject);
        String userId = extractUserId(claims, email, providerSubject);

        if (providerSubject != null) {
            return userRepository.findByProviderSubject(providerSubject)
                .map(existing -> {
                    boolean updated = tryUpdateEmail(existing, email);
                    return updated ? userRepository.save(existing) : existing;
                })
                .orElseGet(() -> upsertByEmailOrCreate(email, userId, providerSubject));
        }

        return upsertByEmailOrCreate(email, userId, null);
    }

    private User upsertByEmailOrCreate(String email, String userId, String providerSubject) {
        return userRepository.findByEmail(email)
            .map(existing -> {
                boolean updated = false;
                if (providerSubject != null && (existing.getProviderSubject() == null || existing.getProviderSubject().isBlank())) {
                    existing.setProviderSubject(providerSubject);
                    updated = true;
                }
                return updated ? userRepository.save(existing) : existing;
            })
            .orElseGet(() -> {
                User user = new User();
                user.setEmail(email);
                user.setProviderSubject(providerSubject);
                user.setUserId(ensureUniqueUserId(userId));
                user.setHashedPassword("oauth2:" + UUID.randomUUID());
                user.setIsActive(true);
                return userRepository.save(user);
            });
    }

    private String extractProviderSubject(Map<String, Object> claims) {
        String sub = claimAsString(claims, "sub");
        if (sub == null || sub.isBlank()) {
            return null;
        }
        return sub;
    }

    private String extractEmail(Map<String, Object> claims, String providerSubject) {
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

        if (providerSubject != null) {
            return providerSubject + "@cognito.local";
        }

        throw new IllegalArgumentException("Missing email claim from authentication token");
    }

    private String extractUserId(Map<String, Object> claims, String fallbackEmail, String providerSubject) {
        if (providerSubject != null && !providerSubject.isBlank()) {
            return sanitizeUserId(providerSubject);
        }
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

    private boolean shouldReplaceEmail(String currentEmail, String incomingEmail) {
        if (incomingEmail == null || incomingEmail.isBlank()) {
            return false;
        }
        if (Objects.equals(currentEmail, incomingEmail)) {
            return false;
        }
        if (currentEmail == null || currentEmail.isBlank()) {
            return true;
        }
        return isSyntheticCognitoEmail(currentEmail) && !isSyntheticCognitoEmail(incomingEmail);
    }

    private boolean tryUpdateEmail(User existing, String incomingEmail) {
        if (!shouldReplaceEmail(existing.getEmail(), incomingEmail)) {
            return false;
        }
        return userRepository.findByEmail(incomingEmail)
            .map(other -> Objects.equals(other.getUserId(), existing.getUserId()))
            .orElse(true)
            && setEmailIfChanged(existing, incomingEmail);
    }

    private boolean setEmailIfChanged(User existing, String incomingEmail) {
        if (Objects.equals(existing.getEmail(), incomingEmail)) {
            return false;
        }
        existing.setEmail(incomingEmail);
        return true;
    }

    private boolean isSyntheticCognitoEmail(String email) {
        return email != null && email.endsWith("@cognito.local");
    }

    private String claimAsString(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }
}
