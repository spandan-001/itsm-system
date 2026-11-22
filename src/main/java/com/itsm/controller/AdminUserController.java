package com.itsm.controller;

import java.util.Locale;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import com.itsm.enums.IncidentCategory;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    enum Role { EMPLOYEE, TECHNICIAN }
    record NewUser(@NotBlank @Size(max=100) String name, @Email @NotBlank @Size(max=255) String email,
                   @NotBlank @Size(min=12,max=72) String password, @NotNull Role role, IncidentCategory domain) {}

    private final RestClient auth;
    private final String secret;

    AdminUserController(@Value("${itsm.auth.issuer}") String issuer,
                        @Value("${SUPABASE_SECRET_KEY:}") String secret) {
        this.secret = secret;
        this.auth = RestClient.create(issuer);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> create(@Valid @RequestBody NewUser user) {
        if (secret.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Configure the server's Supabase secret key to create accounts.");
        if (user.role() == Role.TECHNICIAN && user.domain() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a technician specialty.");
        var profile = Map.of("name", user.name().strip(), "role", user.role().name(),
                "domain", user.domain() == null ? "" : user.domain().name());
        try {
            auth.post().uri("/admin/users").header("apikey", secret)
                .body(Map.of("email", user.email().strip().toLowerCase(Locale.ROOT), "password", user.password(),
                        "email_confirm", true, "app_metadata", Map.of("itsm_profile", profile)))
                .retrieve().toBodilessEntity();
            return Map.of("role", user.role().name());
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 422)
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Check the email and password or use an unused address.");
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Supabase could not create the account.");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not reach Supabase.");
        }
    }
}
