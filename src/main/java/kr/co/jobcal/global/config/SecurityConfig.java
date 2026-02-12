package kr.co.jobcal.global.config;

import kr.co.jobcal.global.oauth.CookieBearerTokenResolver;
import kr.co.jobcal.global.oauth.CookieOAuth2AuthorizationRequestRepository;
import kr.co.jobcal.global.oauth.CustomAuthorizationRequestResolver;
import kr.co.jobcal.global.oauth.CustomOidcUserService;
import kr.co.jobcal.global.oauth.OAuth2AuthenticationFailureHandler;
import kr.co.jobcal.global.oauth.OAuth2AuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        CustomOidcUserService oidcUserService,
        OAuth2AuthenticationSuccessHandler successHandler,
        OAuth2AuthenticationFailureHandler failureHandler,
        Environment environment,
        CookieOAuth2AuthorizationRequestRepository authorizationRequestRepository,
        ClientRegistrationRepository clientRegistrationRepository
    ) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(
                        "/api/",
                        "/api/health",
                        "/api/logout",
                        "/error",
                        "/actuator/health").permitAll()
                .requestMatchers(
                        "/api/oauth2/**",
                        "/api/login/**",
                        "/api/login/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .logout(logout -> logout
                .logoutUrl("/api/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(HttpStatus.OK.value());
                })
            );

        if (hasText(environment.getProperty("spring.security.oauth2.client.registration.cognito.client-id"))) {
            http.oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(auth -> auth
                    .authorizationRequestRepository(authorizationRequestRepository)
                    .authorizationRequestResolver(
                        new CustomAuthorizationRequestResolver(clientRegistrationRepository, "/api/oauth2/authorization")
                    )
                )
                .redirectionEndpoint(redirection -> redirection
                    .baseUri("/api/login/oauth2/code/*")
                )
                .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService))
                .successHandler(successHandler)
                .failureHandler(failureHandler)
            );
        }

        if (hasText(environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri"))) {
            http.oauth2ResourceServer(oauth2 -> oauth2
                .bearerTokenResolver(bearerTokenResolver())
                .jwt(jwt -> {})
            );
        }

        return http.build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        return new CookieBearerTokenResolver("accessToken");
    }
}
