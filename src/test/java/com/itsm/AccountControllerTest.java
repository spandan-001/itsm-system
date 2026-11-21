package com.itsm;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

@SpringBootTest
@ActiveProfiles("test")
class AccountControllerTest {

    private static final String USER_ID = "e304dc94-6169-4263-985a-c822e6481096";

    @Autowired WebApplicationContext context;
    @MockitoBean JwtDecoder jwtDecoder;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(new DelegatingFilterProxy("springSecurityFilterChain", context))
                .build();
    }

    @Test
    void requiresValidBearerToken() throws Exception {
        when(jwtDecoder.decode("invalid")).thenThrow(new BadJwtException("Invalid token"));

        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsIdentityFromVerifiedToken() throws Exception {
        when(jwtDecoder.decode("verified")).thenReturn(Jwt.withTokenValue("verified")
                .header("alg", "ES256")
                .subject(USER_ID)
                .claim("email", "reporter@example.test")
                .build());

        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer verified"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.email").value("reporter@example.test"));
    }
}
