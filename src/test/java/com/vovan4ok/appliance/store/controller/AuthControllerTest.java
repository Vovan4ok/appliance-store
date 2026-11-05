package com.vovan4ok.appliance.store.controller;

import com.vovan4ok.appliance.store.config.SecurityConfig;
import com.vovan4ok.appliance.store.metrics.StoreMetrics;
import com.vovan4ok.appliance.store.security.JwtUtils;
import com.vovan4ok.appliance.store.security.TokenDenylist;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "jwt.expiration-ms=3600000")
public class AuthControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AuthenticationManager authenticationManager;

    @MockitoBean
    UserDetailsService userDetailsService;

    @Autowired
    JwtUtils jwtUtils;

    @MockitoBean
    TokenDenylist tokenDenylist;

    @MockitoBean
    StoreMetrics metrics;

    @Test
    void login_validCredentials_setsJwtCookieWithConfiguredMaxAge() throws Exception {
        var mockAuth = new UsernamePasswordAuthenticationToken(
                "user@test.com", null, List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
        when(authenticationManager.authenticate(any())).thenReturn(mockAuth);

        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .param("username", "user@test.com")
                .param("password", "Password1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(cookie().exists("jwt"))
                .andExpect(cookie().httpOnly("jwt", true))
                .andExpect(cookie().maxAge("jwt", 3600))
                .andExpect(cookie().attribute("jwt", "SameSite", "Lax"))
                .andExpect(cookie().secure("jwt", false));

        verify(metrics).login(StoreMetrics.Channel.WEB, true);
    }

    @Test
    void login_invalidCredentials_redirectsToLoginError() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .param("username", "user@test.com")
                .param("password", "wrong"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(cookie().doesNotExist("jwt"));

        verify(metrics).login(StoreMetrics.Channel.WEB, false);
    }

    @Test
    void logout_clearsJwtCookie() throws Exception {
        mockMvc.perform(post("/auth/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"))
                .andExpect(cookie().maxAge("jwt", 0));
    }

    @Test
    void logout_withValidCookie_revokesTokenForItsRemainingLifetime() throws Exception {
        String token = jwtUtils.generateToken("user@test.com", "ROLE_CLIENT");

        mockMvc.perform(post("/auth/logout").with(csrf()).cookie(new Cookie("jwt", token)))
                .andExpect(redirectedUrl("/login?logout"))
                .andExpect(cookie().maxAge("jwt", 0));

        verify(tokenDenylist).revoke(eq(jwtUtils.extractId(token)),
                argThat(ttl -> ttl.toSeconds() > 3500 && ttl.toSeconds() <= 3600));
        verify(metrics).logout(StoreMetrics.Channel.WEB);
    }

    @Test
    void logout_withoutCookie_revokesNothing() throws Exception {
        mockMvc.perform(post("/auth/logout").with(csrf()))
                .andExpect(redirectedUrl("/login?logout"));

        verify(tokenDenylist, never()).revoke(any(), any());
        verify(metrics, never()).logout(any());
    }

    @Test
    void revokedToken_isTreatedAsLoggedOut() throws Exception {
        String token = jwtUtils.generateToken("user@test.com", "ROLE_CLIENT");
        when(tokenDenylist.isRevoked(jwtUtils.extractId(token))).thenReturn(true);

        mockMvc.perform(get("/orders").cookie(new Cookie("jwt", token)))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void validToken_isAccepted() throws Exception {
        String token = jwtUtils.generateToken("user@test.com", "ROLE_CLIENT");

        // Authenticated, so security lets it through (no controller here, hence not a login redirect)
        mockMvc.perform(get("/orders").cookie(new Cookie("jwt", token)))
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl()).isNull());
    }
}
