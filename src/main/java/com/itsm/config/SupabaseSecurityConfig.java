package com.itsm.config;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import com.itsm.service.AdminAccess;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SupabaseSecurityConfig {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, AdminAccess admins) {
        return http.securityMatcher("/api/**")
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.GET, "/api/me", "/api/services", "/api/incidents",
                                "/api/incidents/search", "/api/incidents/{id:[0-9]+}", "/api/incidents/{id}/audit",
                                "/api/incidents/{id}/sla", "/api/notifications",
                                "/api/notifications/unread-count", "/api/incidents/{id}/notifications",
                                "/api/sla/policies", "/api/technicians/me", "/api/incidents/{id}/notes",
                                "/api/incidents/{id}/suggested-resolutions").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/incidents", "/api/incidents/{id}/notes",
                                "/api/incidents/{id}/feedback", "/api/incidents/classify").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/incidents/{id}/status",
                                "/api/notifications/{id}/read", "/api/technicians/{id}/availability").authenticated()
                        .requestMatchers("/api/admin/**", "/api/audit", "/api/analytics/**", "/api/routing/**",
                                "/api/technicians/**", "/api/strategies/**", "/api/sla/**", "/api/classifier/**", "/api/outages", "/api/outages/**",
                                "/api/incidents/similar", "/api/incidents/{id}/assignment",
                                "/api/incidents/{id}/recommendations", "/api/incidents/{id}/duplicates",
                                "/api/incidents/{id}/not-duplicate")
                        .access((authentication, context) -> new AuthorizationDecision(
                                authentication.get().getPrincipal() instanceof Jwt jwt
                                        && admins.allows(jwt.getSubject())))
                        .anyRequest().denyAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    JwtDecoder supabaseJwtDecoder(@Value("${itsm.auth.issuer}") String issuer) {
        var decoder = NimbusJwtDecoder.withJwkSetUri(issuer + "/.well-known/jwks.json")
                .jwsAlgorithm(SignatureAlgorithm.ES256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                jwt -> jwt.getAudience() != null && jwt.getAudience().contains("authenticated")
                        && "authenticated".equals(jwt.getClaimAsString("role"))
                        && jwt.getExpiresAt() != null && isUserId(jwt.getSubject())
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"))));
        return decoder;
    }

    private static boolean isUserId(String subject) {
        try {
            return subject != null && UUID.fromString(subject).toString().equalsIgnoreCase(subject);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
