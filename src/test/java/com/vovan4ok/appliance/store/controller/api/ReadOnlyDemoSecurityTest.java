package com.vovan4ok.appliance.store.controller.api;

import com.vovan4ok.appliance.store.config.SecurityConfig;
import com.vovan4ok.appliance.store.metrics.StoreMetrics;
import com.vovan4ok.appliance.store.model.dto.api.AiDescriptionResponse;
import com.vovan4ok.appliance.store.security.TokenDenylist;
import com.vovan4ok.appliance.store.service.AiDescriptionService;
import com.vovan4ok.appliance.store.service.AiRateLimiter;
import com.vovan4ok.appliance.store.service.ApplianceService;
import com.vovan4ok.appliance.store.service.ManufacturerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The read-only demo filter in the real security chain: a sample employee login can read, but its changes (and
 * the paid AI calls) never reach a controller; other employees are unaffected.
 */
@WebMvcTest(ApplianceApiController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.demo.read-only-accounts=phobos@gmail.com,moon@gmail.com")
class ReadOnlyDemoSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ApplianceService applianceService;
    @MockitoBean
    ManufacturerService manufacturerService;
    @MockitoBean
    AiDescriptionService aiDescriptionService;
    @MockitoBean
    AiRateLimiter aiRateLimiter;
    @MockitoBean
    StoreMetrics metrics;
    @MockitoBean
    UserDetailsService userDetailsService;
    @MockitoBean
    TokenDenylist tokenDenylist;

    @Test
    @WithMockUser(username = "phobos@gmail.com", roles = "EMPLOYEE")
    void demoEmployee_canRead() throws Exception {
        when(applianceService.findAll(any(), any(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/appliances")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "phobos@gmail.com", roles = "EMPLOYEE")
    void demoEmployee_cannotUseTheAi() throws Exception {
        mockMvc.perform(post("/api/v1/appliances/1/ai-description"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("This is a read-only demo account"));

        verify(aiRateLimiter, never()).acquire(any());
        verify(aiDescriptionService, never()).generate(any(), any());
    }

    @Test
    @WithMockUser(username = "moon@gmail.com", roles = "EMPLOYEE")
    void demoEmployee_cannotDelete() throws Exception {
        mockMvc.perform(delete("/api/v1/appliances/1")).andExpect(status().isForbidden());

        verify(applianceService, never()).delete(any());
    }

    @Test
    @WithMockUser(username = "owner@example.com", roles = "EMPLOYEE")
    void otherEmployee_isNotRestricted() throws Exception {
        when(aiDescriptionService.generate(eq(1L), any(Locale.class))).thenReturn(new AiDescriptionResponse(
                1L, "en", "Crispy toast every morning.", AiDescriptionResponse.Source.AI, null));

        mockMvc.perform(post("/api/v1/appliances/1/ai-description")).andExpect(status().isOk());
    }
}
