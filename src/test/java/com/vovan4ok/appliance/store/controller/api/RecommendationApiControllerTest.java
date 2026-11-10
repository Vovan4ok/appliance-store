package com.vovan4ok.appliance.store.controller.api;

import com.vovan4ok.appliance.store.config.SecurityConfig;
import com.vovan4ok.appliance.store.exception.RateLimitExceededException;
import com.vovan4ok.appliance.store.model.Appliance;
import com.vovan4ok.appliance.store.model.Category;
import com.vovan4ok.appliance.store.model.Manufacturer;
import com.vovan4ok.appliance.store.model.Recommendation;
import com.vovan4ok.appliance.store.model.RecommendationReason;
import com.vovan4ok.appliance.store.security.TokenDenylist;
import com.vovan4ok.appliance.store.service.RecommendationRateLimiter;
import com.vovan4ok.appliance.store.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * With the real security rules: the endpoint must work without any login, also from other websites.
 */
@WebMvcTest(RecommendationApiController.class)
@Import(SecurityConfig.class)
class RecommendationApiControllerTest {

    private static final String URL = "/api/v1/recommendations";
    private static final String OTHER_SITE = "https://blog.example.com";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RecommendationService recommendationService;
    @MockitoBean
    RecommendationRateLimiter rateLimiter;
    @MockitoBean
    UserDetailsService userDetailsService;
    @MockitoBean
    TokenDenylist tokenDenylist;

    @Test
    void anonymous_getsRecommendations_withScoreAndReasonCodes() throws Exception {
        when(recommendationService.recommend(1L, 3)).thenReturn(List.of(recommendation()));

        mockMvc.perform(get(URL).param("applianceId", "1").param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(18))
                .andExpect(jsonPath("$[0].name").value("Dishwasher"))
                .andExpect(jsonPath("$[0].manufacturer").value("Bosch"))
                .andExpect(jsonPath("$[0].manufacturerLogo").value("/uploads/bosch.png"))
                .andExpect(jsonPath("$[0].price").value(699.99))
                .andExpect(jsonPath("$[0].score").value(4.5))
                .andExpect(jsonPath("$[0].reasons[0]").value("BOUGHT_TOGETHER"));
    }

    @Test
    void publicResponse_hidesTheStockCountAndDescriptions() throws Exception {
        when(recommendationService.recommend(1L, 4)).thenReturn(List.of(recommendation()));

        mockMvc.perform(get(URL).param("applianceId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stock").doesNotExist())
                .andExpect(jsonPath("$[0].description").doesNotExist())
                .andExpect(jsonPath("$[0].characteristic").doesNotExist());
    }

    @Test
    void withoutApplianceId_asksForBestSellers_withTheDefaultLimit() throws Exception {
        when(recommendationService.recommend(isNull(), anyInt())).thenReturn(List.of());

        mockMvc.perform(get(URL)).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());

        verify(recommendationService).recommend(null, 4);
    }

    @Test
    void response_mayBeCachedForFiveMinutes() throws Exception {
        when(recommendationService.recommend(isNull(), anyInt())).thenReturn(List.of());

        mockMvc.perform(get(URL))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=300, public"));
    }

    @Test
    void unknownProduct_returns404() throws Exception {
        when(recommendationService.recommend(99L, 4))
                .thenThrow(new IllegalArgumentException("Appliance not found: 99"));

        mockMvc.perform(get(URL).param("applianceId", "99")).andExpect(status().isNotFound());
    }

    @Test
    void badApplianceId_returns400() throws Exception {
        mockMvc.perform(get(URL).param("applianceId", "abc")).andExpect(status().isBadRequest());
    }

    @Test
    void overTheLimit_returns429_withRetryAfter_withoutQueryingTheDatabase() throws Exception {
        doThrow(new RateLimitExceededException(42)).when(rateLimiter).acquire(anyString());

        mockMvc.perform(get(URL).header(HttpHeaders.ORIGIN, OTHER_SITE))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "42"))
                // Even the error is readable by the widget on the other site
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, OTHER_SITE))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Retry-After"));

        verify(recommendationService, never()).recommend(org.mockito.ArgumentMatchers.any(), anyInt());
    }

    @Test
    void rateLimit_isPerClientAddress() throws Exception {
        when(recommendationService.recommend(isNull(), anyInt())).thenReturn(List.of());

        mockMvc.perform(get(URL).with(request -> {
            request.setRemoteAddr("203.0.113.7");
            return request;
        })).andExpect(status().isOk());

        verify(rateLimiter).acquire("203.0.113.7");
    }

    @Test
    void otherWebsite_mayReadIt_withoutCredentials() throws Exception {
        when(recommendationService.recommend(isNull(), anyInt())).thenReturn(List.of());

        mockMvc.perform(get(URL).header(HttpHeaders.ORIGIN, OTHER_SITE))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, OTHER_SITE))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void preflight_allowsOnlyGet() throws Exception {
        mockMvc.perform(options(URL)
                        .header(HttpHeaders.ORIGIN, OTHER_SITE)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"));

        mockMvc.perform(options(URL)
                        .header(HttpHeaders.ORIGIN, OTHER_SITE)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE"))
                .andExpect(status().isForbidden());
    }

    @Test
    void restOfTheApi_staysClosedToOtherWebsites() throws Exception {
        mockMvc.perform(options("/api/v1/appliances")
                        .header(HttpHeaders.ORIGIN, OTHER_SITE)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void writing_isNotPublic() throws Exception {
        mockMvc.perform(post(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Authentication required")));
    }

    private static Recommendation recommendation() {
        Manufacturer bosch = new Manufacturer();
        bosch.setId(1L);
        bosch.setName("Bosch");
        bosch.setLogoPath("/uploads/bosch.png");
        Appliance a = new Appliance();
        a.setId(18L);
        a.setName("Dishwasher");
        a.setModel("SMS4");
        a.setCategory(Category.BIG);
        a.setManufacturer(bosch);
        a.setPrice(new BigDecimal("699.99"));
        a.setStock(3);
        a.setDescription("Quiet");
        a.setCharacteristic("45 dB");
        return new Recommendation(a, 4.5, List.of(RecommendationReason.BOUGHT_TOGETHER));
    }
}
