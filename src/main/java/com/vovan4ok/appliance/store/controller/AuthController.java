package com.vovan4ok.appliance.store.controller;

import com.vovan4ok.appliance.store.security.JwtUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

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

            return "redirect:/";
        } catch (AuthenticationException e) {
            return "redirect:/login?error";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/login?logout";
    }
}
