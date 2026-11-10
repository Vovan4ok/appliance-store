package com.vovan4ok.appliance.store.config;

import com.vovan4ok.appliance.store.security.JwtAuthenticationFilter;
import com.vovan4ok.appliance.store.security.JwtUtils;
import com.vovan4ok.appliance.store.security.TokenDenylist;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    // Websites allowed to call the public recommendations endpoint from the browser (the embeddable widget)
    @Value("${app.widget.allowed-origins}")
    private List<String> widgetAllowedOrigins;

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

    /**
     * Cross-origin access for the recommendations endpoint only: GET, no cookies or other credentials (the data
     * is public anyway), allowed origins from {@code app.widget.allowed-origins}. Browsers cache the preflight
     * answer for an hour.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration widget = new CorsConfiguration();
        widget.setAllowedOriginPatterns(widgetAllowedOrigins);
        widget.setAllowedMethods(List.of("GET"));
        widget.setAllowedHeaders(List.of("Accept", "Accept-Language"));
        // Lets the widget read how long to wait after a 429
        widget.setExposedHeaders(List.of(HttpHeaders.RETRY_AFTER));
        widget.setAllowCredentials(false);
        widget.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/recommendations", widget);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, TokenDenylist tokenDenylist) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtUtils(), tokenDenylist);

        http
                .securityContext(ctx -> ctx
                        .securityContextRepository(new RequestAttributeSecurityContextRepository()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // CORS only where corsConfigurationSource() registers it; everything else stays same-origin
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public MVC and static
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/", "/login", "/register", "/auth/login", "/auth/logout",
                                "/favicon.ico", "/uploads/**").permitAll()
                        // Health check (Docker / load balancers) and metrics for Prometheus; no other actuator
                        // endpoint is exposed. In prod both live only on the internal management port (8081).
                        .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                        // Swagger UI + OpenAPI docs
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // REST API — recommendations are public and read-only (the embeddable widget)
                        .requestMatchers(HttpMethod.GET, "/api/v1/recommendations").permitAll()
                        // REST API — auth endpoint is public
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        // REST API — write operations require EMPLOYEE
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/appliances", "/api/v1/manufacturers",
                                "/api/v1/appliances/*/ai-description").hasRole("EMPLOYEE")
                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/appliances/**", "/api/v1/manufacturers/**").hasRole("EMPLOYEE")
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/v1/appliances/**", "/api/v1/manufacturers/**").hasRole("EMPLOYEE")
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/**").hasRole("EMPLOYEE")
                        // REST API — all other endpoints require authentication
                        .requestMatchers("/api/v1/**").authenticated()
                        // MVC routes
                        .requestMatchers(HttpMethod.GET, "/manufacturers", "/manufacturers/*").authenticated()
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
                                response.getWriter().write(
                                        "{\"status\":401,\"error\":\"Unauthorized\","
                                        + "\"message\":\"Authentication required\"}");
                            } else {
                                response.sendRedirect(request.getContextPath() + "/login");
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write(
                                        "{\"status\":403,\"error\":\"Forbidden\","
                                        + "\"message\":\"Access denied\"}");
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