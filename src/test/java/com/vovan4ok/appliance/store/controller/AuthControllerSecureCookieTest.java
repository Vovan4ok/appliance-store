package com.vovan4ok.appliance.store.controller;

import com.vovan4ok.appliance.store.config.SecurityConfig;
import com.vovan4ok.appliance.store.metrics.StoreMetrics;
import com.vovan4ok.appliance.store.security.TokenDenylist;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;

/**
 * Behind HTTPS (Cloud Run sets COOKIE_SECURE=true) the JWT cookie is Secure, so it never travels over plain HTTP.
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.security.cookie-secure=true")
class AuthControllerSecureCookieTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AuthenticationManager authenticationManager;
    @MockitoBean
    UserDetailsService userDetailsService;
    @MockitoBean
    TokenDenylist tokenDenylist;
    @MockitoBean
    StoreMetrics metrics;

    @Test
    void loginAndLogout_setSecureCookies() throws Exception {
        when(authenticationManager.authenticate(any())).thenReturn(new UsernamePasswordAuthenticationToken(
                "user@test.com", null, List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))));

        mockMvc.perform(post("/auth/login").with(csrf())
                        .param("username", "user@test.com")
                        .param("password", "Password1!"))
                .andExpect(cookie().secure("jwt", true))
                .andExpect(cookie().httpOnly("jwt", true))
                .andExpect(cookie().attribute("jwt", "SameSite", "Lax"));

        mockMvc.perform(post("/auth/logout").with(csrf()))
                .andExpect(cookie().secure("jwt", true))
                .andExpect(cookie().maxAge("jwt", 0));
    }
}
