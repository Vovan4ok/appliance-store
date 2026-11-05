package com.vovan4ok.appliance.store.controller.api;

import com.vovan4ok.appliance.store.metrics.StoreMetrics;
import com.vovan4ok.appliance.store.service.AiDescriptionService;
import com.vovan4ok.appliance.store.service.AiRateLimiter;
import com.vovan4ok.appliance.store.service.ApplianceService;
import com.vovan4ok.appliance.store.service.ManufacturerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring's own request errors are the client's fault: they keep their 4xx status and the API's JSON error format
 * instead of becoming a 500 with a stack trace in the log.
 */
@WebMvcTest(ApplianceApiController.class)
@WithMockUser(roles = "EMPLOYEE")
class ApiRequestErrorTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ApplianceService applianceService;

    @MockitoBean
    ManufacturerService manufacturerService;

    @MockitoBean
    UserDetailsService userDetailsService;

    @MockitoBean
    AiRateLimiter aiRateLimiter;

    @MockitoBean
    AiDescriptionService aiDescriptionService;

    @MockitoBean
    StoreMetrics metrics;

    @Test
    void wrongHttpMethod_returns405AsJsonWithAllowHeader() throws Exception {
        // /api/v1/appliances/{id} supports GET, PUT and DELETE only
        mockMvc.perform(post("/api/v1/appliances/3").with(csrf()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string(HttpHeaders.ALLOW, containsString("GET")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Method Not Allowed"))
                .andExpect(jsonPath("$.message").value(containsString("POST")))
                .andExpect(jsonPath("$.path").value("/api/v1/appliances/3"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void nonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/appliances/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid value for 'id'"));

        verify(applianceService, never()).findById(any());
    }

    @Test
    void malformedJsonBody_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/appliances/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void unsupportedContentType_returns415() throws Exception {
        mockMvc.perform(put("/api/v1/appliances/1").with(csrf())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }
}
