package com.vovan4ok.appliance.store.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ReadOnlyDemoFilterTest {

    private final ReadOnlyDemoFilter filter = new ReadOnlyDemoFilter(
            new ReadOnlyDemoAccounts(List.of(" Phobos@gmail.com ", "", "mercury@gmail.com")));
    private final FilterChain chain = mock(FilterChain.class);
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void clearUser() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "{0} changes data: {1}")
    @CsvSource({"POST, true", "PUT, true", "DELETE, true", "PATCH, true",
            "GET, false", "HEAD, false", "OPTIONS, false"})
    void changesData(String method, boolean expected) {
        assertThat(ReadOnlyDemoFilter.changesData(method)).isEqualTo(expected);
    }

    @Test
    void demoAccount_pageChange_goesBackToThePreviousPage_withTheBanner() throws Exception {
        loginAs("phobos@gmail.com");
        MockHttpServletRequest request = request("POST", "/profile/change-password");
        request.addHeader("Referer", "http://localhost/profile");

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getRedirectedUrl()).isEqualTo("/profile?readOnlyDemo=1");
    }

    @Test
    void demoAccount_keepsTheQueryOfThePreviousPage() throws Exception {
        loginAs("phobos@gmail.com");
        MockHttpServletRequest request = request("POST", "/appliances/5/delete");
        request.addHeader("Referer", "http://localhost/appliances?page=2&sortBy=price");

        filter.doFilter(request, response, chain);

        assertThat(response.getRedirectedUrl()).isEqualTo("/appliances?page=2&sortBy=price&readOnlyDemo=1");
    }

    @Test
    void demoAccount_neverRedirectsToAnotherSite() throws Exception {
        loginAs("phobos@gmail.com");
        MockHttpServletRequest request = request("POST", "/shop/add-to-cart");
        request.addHeader("Referer", "https://evil.example.com/phish");

        filter.doFilter(request, response, chain);

        assertThat(response.getRedirectedUrl()).isEqualTo("/?readOnlyDemo=1");
    }

    @Test
    void demoAccount_withoutReferer_goesHome() throws Exception {
        loginAs("phobos@gmail.com");

        filter.doFilter(request("POST", "/orders/1/edit"), response, chain);

        assertThat(response.getRedirectedUrl()).isEqualTo("/?readOnlyDemo=1");
    }

    @Test
    void demoAccount_apiChange_gets403Json() throws Exception {
        loginAs("phobos@gmail.com");

        filter.doFilter(request("POST", "/api/v1/appliances/1/ai-description"), response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("read-only demo account");
    }

    @Test
    void demoAccount_mayStillRead() throws Exception {
        loginAs("phobos@gmail.com");

        filter.doFilter(request("GET", "/appliances"), response, chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    void demoAccount_mayStillLogInAndOut() throws Exception {
        loginAs("phobos@gmail.com");

        for (String path : List.of("/auth/login", "/auth/logout", "/api/v1/auth/login", "/api/v1/auth/logout")) {
            filter.doFilter(request("POST", path), new MockHttpServletResponse(), chain);
        }

        verify(chain, times(4)).doFilter(any(), any());
    }

    @Test
    void emails_areMatchedWithoutCaseOrSpaces() throws Exception {
        loginAs("PHOBOS@GMAIL.COM");

        filter.doFilter(request("POST", "/profile/edit"), response, chain);

        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void otherAccounts_areNotRestricted() throws Exception {
        loginAs("owner@example.com");

        filter.doFilter(request("POST", "/orders/3/approved"), response, chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    void anonymousRequests_areLeftToTheSecurityRules() throws Exception {
        filter.doFilter(request("POST", "/register"), response, chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    void noDemoAccountsConfigured_restrictsNobody() throws Exception {
        ReadOnlyDemoFilter unrestricted = new ReadOnlyDemoFilter(new ReadOnlyDemoAccounts(List.of()));
        loginAs("phobos@gmail.com");

        unrestricted.doFilter(request("POST", "/profile/edit"), response, chain);

        verify(chain).doFilter(any(), any());
    }

    private static MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServerName("localhost");
        return request;
    }

    private static void loginAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))));
    }
}
