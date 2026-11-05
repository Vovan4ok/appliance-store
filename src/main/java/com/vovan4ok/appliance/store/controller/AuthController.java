package com.vovan4ok.appliance.store.controller;

import com.vovan4ok.appliance.store.metrics.StoreMetrics;
import com.vovan4ok.appliance.store.metrics.StoreMetrics.Channel;
import com.vovan4ok.appliance.store.security.JwtUtils;
import com.vovan4ok.appliance.store.security.TokenDenylist;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    // true behind HTTPS (Cloud Run): the browser then never sends the token over plain HTTP
    @Value("${app.security.cookie-secure:false}")
    private boolean cookieSecure;

    /**
     * HttpOnly: no JavaScript access. SameSite=Lax: other sites can't make the browser send the token with their
     * POST requests, which matters because CSRF tokens are off (stateless JWT cookie).
     */
    private Cookie jwtCookie(String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie("jwt", value);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }

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

            response.addCookie(jwtCookie(token, (int) (jwtUtils.getExpirationMs() / 1000)));
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
        response.addCookie(jwtCookie("", 0));
        return "redirect:/login?logout";
    }
}
