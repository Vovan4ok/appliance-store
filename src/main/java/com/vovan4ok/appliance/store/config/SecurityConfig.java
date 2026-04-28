package com.vovan4ok.appliance.store.config;

import com.vovan4ok.appliance.store.security.JwtAuthenticationFilter;
import com.vovan4ok.appliance.store.security.JwtUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtUtils jwtUtils() {
        return new JwtUtils(jwtSecret, jwtExpirationMs);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtUtils());

        http
                .securityContext(ctx -> ctx
                        .securityContextRepository(new RequestAttributeSecurityContextRepository()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public MVC and static
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/", "/login", "/register", "/auth/login", "/auth/logout",
                                "/favicon.ico", "/uploads/**").permitAll()
                        // Swagger UI + OpenAPI docs
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // REST API — auth endpoint is public
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        // REST API — write operations require EMPLOYEE
                        .requestMatchers(HttpMethod.POST, "/api/v1/appliances", "/api/v1/manufacturers").hasRole("EMPLOYEE")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/appliances/**", "/api/v1/manufacturers/**").hasRole("EMPLOYEE")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/appliances/**", "/api/v1/manufacturers/**").hasRole("EMPLOYEE")
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/**").hasRole("EMPLOYEE")
                        // REST API — all other endpoints require authentication
                        .requestMatchers("/api/v1/**").authenticated()
                        // MVC routes
                        .requestMatchers("/employees/**", "/manufacturers/**",
                                "/appliances/**", "/clients/**").hasRole("EMPLOYEE")
                        .requestMatchers("/shop/**").hasRole("CLIENT")
                        .requestMatchers("/orders/**").authenticated()
                        .anyRequest().authenticated()
                )
                .requestCache(cache -> cache.disable())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
                            } else {
                                response.sendRedirect(request.getContextPath() + "/login");
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Access denied\"}");
                            } else {
                                response.sendRedirect(request.getContextPath() + "/login");
                            }
                        })
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                );

        return http.build();
    }
}