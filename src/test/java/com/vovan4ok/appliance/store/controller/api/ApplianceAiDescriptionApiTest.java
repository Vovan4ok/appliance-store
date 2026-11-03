package com.vovan4ok.appliance.store.controller.api;

import com.vovan4ok.appliance.store.config.SecurityConfig;
import com.vovan4ok.appliance.store.model.dto.api.AiDescriptionResponse;
import com.vovan4ok.appliance.store.service.AiDescriptionService;
import com.vovan4ok.appliance.store.service.ApplianceService;
import com.vovan4ok.appliance.store.service.ManufacturerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplianceApiController.class)
@Import(SecurityConfig.class)
public class ApplianceAiDescriptionApiTest {
    private static final AiDescriptionResponse AI_RESULT = new AiDescriptionResponse(
            1L, "en", "Crispy toast every morning.", AiDescriptionResponse.Source.AI, null);

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ApplianceService applianceService;
    @MockitoBean
    ManufacturerService manufacturerService;
    @MockitoBean
    AiDescriptionService aiDescriptionService;
    @MockitoBean
    UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employee_getsAiDescription() throws Exception {
        when(aiDescriptionService.generate(eq(1L), any(Locale.class))).thenReturn(AI_RESULT);

        mockMvc.perform(post("/api/v1/appliances/1/ai-description"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("AI"))
                .andExpect(jsonPath("$.description").value("Crispy toast every morning."));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void langParam_isPassedToServiceAsLocale() throws Exception {
        when(aiDescriptionService.generate(eq(1L), any(Locale.class))).thenReturn(AI_RESULT);

        mockMvc.perform(post("/api/v1/appliances/1/ai-description").param("lang", "uk"))
                .andExpect(status().isOk());

        verify(aiDescriptionService).generate(eq(1L), argThat(l -> "uk".equals(l.getLanguage())));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void unknownAppliance_returns404() throws Exception {
        when(aiDescriptionService.generate(eq(99L), any(Locale.class)))
                .thenThrow(new IllegalArgumentException("Appliance not found: 99"));

        mockMvc.perform(post("/api/v1/appliances/99/ai-description"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void client_isForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/appliances/1/ai-description"))
                .andExpect(status().isForbidden());

        verify(aiDescriptionService, never()).generate(any(), any());
    }

    @Test
    void anonymous_isUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/appliances/1/ai-description"))
                .andExpect(status().isUnauthorized());
    }
}
