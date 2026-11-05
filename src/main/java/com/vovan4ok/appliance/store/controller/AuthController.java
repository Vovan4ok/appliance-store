package com.vovan4ok.appliance.store.controller;

import com.vovan4ok.appliance.store.metrics.StoreMetrics;
import com.vovan4ok.appliance.store.metrics.StoreMetrics.Channel;
import com.vovan4ok.appliance.store.security.JwtUtils;
import com.vovan4ok.appliance.store.security.TokenDenylist;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final TokenDenylist tokenDenylist;
    private final StoreMetrics metrics;

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpServletResponse response) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));

            String role = auth.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority())
                    .orElse("ROLE_CLIENT");

            String token = jwtUtils.generateToken(username, role);

            Cookie cookie = new Cookie("jwt", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge((int) (jwtUtils.getExpirationMs() / 1000));
            response.addCookie(cookie);
            metrics.login(Channel.WEB, true);

            return "redirect:/";
        } catch (AuthenticationException e) {
            metrics.login(Channel.WEB, false);
            return "redirect:/login?error";
        }
    }

    @PostMapping("/logout")
    public String logout(@CookieValue(name = "jwt", required = false) String token,
                         HttpServletResponse response) {
        // Deleting the cookie is not enough: a copied token would stay valid until it expires
        if (token != null && jwtUtils.isValid(token)) {
            tokenDenylist.revoke(jwtUtils.extractId(token), jwtUtils.remainingLifetime(token));
            metrics.logout(Channel.WEB);
        }
        Cookie cookie = new Cookie("jwt", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/login?logout";
    }
}
