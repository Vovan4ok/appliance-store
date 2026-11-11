package com.vovan4ok.appliance.store.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Stops every change made with a read-only demo account ({@link ReadOnlyDemoAccounts}) before it reaches a
 * controller. A change is any request that is not GET/HEAD/OPTIONS, plus the GET links that change data
 * (delete, remove, approve, unapprove). Logging in and out stays possible.
 *
 * <p>REST API: 403 with a JSON error. Pages: back to the page the user came from, with {@code ?readOnlyDemo}
 * so it shows a banner.
 */
@Slf4j
public class ReadOnlyDemoFilter extends OncePerRequestFilter {

    public static final String BANNER_PARAM = "readOnlyDemo";

    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private static final Set<String> ALWAYS_ALLOWED = Set.of(
            "/auth/login", "/auth/logout", "/api/v1/auth/login", "/api/v1/auth/logout");

    // GET links that change data: /appliances/5/delete, /orders/3/rows/7/remove, /orders/3/approved ...
    private static final Pattern CHANGING_GET = Pattern.compile("^/.+/(delete|remove|approved|unapproved)/?$");

    private final ReadOnlyDemoAccounts accounts;

    public ReadOnlyDemoFilter(ReadOnlyDemoAccounts accounts) {
        this.accounts = accounts;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String path = request.getRequestURI().substring(request.getContextPath().length());

        if (auth == null || !auth.isAuthenticated() || !accounts.isReadOnly(auth.getName())
                || !changesData(request.getMethod(), path) || ALWAYS_ALLOWED.contains(path)) {
            chain.doFilter(request, response);
            return;
        }

        log.info("Blocked {} {} by read-only demo account {}", request.getMethod(), path, auth.getName());
        if (path.startsWith("/api/")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\","
                    + "\"message\":\"This is a read-only demo account\"}");
        } else {
            response.sendRedirect(backWithBanner(request));
        }
    }

    static boolean changesData(String method, String path) {
        return !READ_METHODS.contains(method) || CHANGING_GET.matcher(path).matches();
    }

    /** The page the user came from (only if it is on this site), else the home page, with the banner flag. */
    private static String backWithBanner(HttpServletRequest request) {
        String target = request.getContextPath() + "/";
        String referer = request.getHeader("Referer");
        if (referer != null) {
            try {
                URI uri = URI.create(referer);
                if (request.getServerName().equalsIgnoreCase(uri.getHost()) && uri.getRawPath() != null
                        && uri.getRawPath().startsWith("/")) {
                    target = uri.getRawPath() + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "");
                }
            } catch (IllegalArgumentException e) {
                // A malformed Referer: fall back to the home page
            }
        }
        return UriComponentsBuilder.fromUriString(target).replaceQueryParam(BANNER_PARAM, "1").build().toUriString();
    }
}
