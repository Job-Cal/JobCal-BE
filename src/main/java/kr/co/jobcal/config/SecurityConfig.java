package kr.co.jobcal.config;

import kr.co.jobcal.security.CustomOidcUserService;
import kr.co.jobcal.security.OAuth2AuthenticationFailureHandler;
import kr.co.jobcal.security.OAuth2AuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        CustomOidcUserService oidcUserService,
        OAuth2AuthenticationSuccessHandler successHandler,
        OAuth2AuthenticationFailureHandler failureHandler,
        Environment environment
    ) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(
                        "/api/",
                        "/api/health",
                        "/error",
                        "/actuator/health").permitAll()
                .requestMatchers(
                        "/oauth2/**",
                        "/login/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(HttpStatus.OK.value());
                })
            );

        if (hasText(environment.getProperty("spring.security.oauth2.client.registration.cognito.client-id"))) {
            http.oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService))
                .successHandler(successHandler)
                .failureHandler(failureHandler)
            );
        }

        if (hasText(environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri"))) {
            http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {})
            );
        }

        return http.build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
