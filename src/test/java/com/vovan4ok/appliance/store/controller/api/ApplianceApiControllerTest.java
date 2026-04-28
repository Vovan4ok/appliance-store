package com.vovan4ok.appliance.store.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vovan4ok.appliance.store.model.Appliance;
import com.vovan4ok.appliance.store.model.Category;
import com.vovan4ok.appliance.store.model.Manufacturer;
import com.vovan4ok.appliance.store.model.PowerType;
import com.vovan4ok.appliance.store.model.dto.api.ApplianceRequest;
import com.vovan4ok.appliance.store.service.ApplianceService;
import com.vovan4ok.appliance.store.service.ManufacturerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApplianceApiController.class)
@WithMockUser(roles = "EMPLOYEE")
class ApplianceApiControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    ApplianceService applianceService;

    @MockBean
    ManufacturerService manufacturerService;

    @MockBean
    UserDetailsService userDetailsService;

    private static Manufacturer mfr(Long id, String name) {
        Manufacturer m = new Manufacturer();
        m.setId(id);
        m.setName(name);
        return m;
    }

    private Appliance sampleAppliance() {
        Appliance a = new Appliance();
        a.setId(1L);
        a.setName("Test Appliance");
        a.setModel("TA-100");
        a.setCategory(Category.BIG);
        a.setPowerType(PowerType.AC220);
        a.setManufacturer(mfr(1L, "Samsung"));
        a.setPrice(new BigDecimal("499.99"));
        a.setStock(10);
        return a;
    }

    @Test
    void list_returnsPageOfAppliances() throws Exception {
        when(applianceService.findAll(any(), any(), any(), any(), any(), any(),
                anyBoolean(), anyBoolean(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleAppliance())));

        mockMvc.perform(get("/api/v1/appliances").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Test Appliance"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getById_found_returnsAppliance() throws Exception {
        when(applianceService.findById(1L)).thenReturn(Optional.of(sampleAppliance()));

        mockMvc.perform(get("/api/v1/appliances/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Appliance"))
                .andExpect(jsonPath("$.category").value("BIG"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(applianceService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/appliances/99").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(manufacturerService.findById(1L)).thenReturn(Optional.of(mfr(1L, "Samsung")));
        when(applianceService.save(any())).thenReturn(sampleAppliance());

        ApplianceRequest request = new ApplianceRequest();
        request.setName("Test Appliance");
        request.setModel("TA-100");
        request.setCategory(Category.BIG);
        request.setPowerType(PowerType.AC220);
        request.setManufacturerId(1L);
        request.setPrice(new BigDecimal("499.99"));
        request.setStock(10);

        mockMvc.perform(post("/api/v1/appliances")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Appliance"));
    }

    @Test
    void create_missingRequiredFields_returns400() throws Exception {
        ApplianceRequest request = new ApplianceRequest();

        mockMvc.perform(post("/api/v1/appliances")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(applianceService, never()).save(any());
    }

    @Test
    void update_found_returnsUpdated() throws Exception {
        Appliance updated = sampleAppliance();
        updated.setName("Updated Name");

        when(applianceService.findById(1L)).thenReturn(Optional.of(sampleAppliance()));
        when(manufacturerService.findById(1L)).thenReturn(Optional.of(mfr(1L, "Samsung")));
        when(applianceService.save(any())).thenReturn(updated);

        ApplianceRequest request = new ApplianceRequest();
        request.setName("Updated Name");
        request.setCategory(Category.BIG);
        request.setPowerType(PowerType.AC220);
        request.setManufacturerId(1L);
        request.setPrice(new BigDecimal("499.99"));
        request.setStock(10);

        mockMvc.perform(put("/api/v1/appliances/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        when(applianceService.findById(99L)).thenReturn(Optional.empty());

        ApplianceRequest request = new ApplianceRequest();
        request.setName("Name");
        request.setCategory(Category.BIG);
        request.setPowerType(PowerType.AC220);
        request.setManufacturerId(1L);
        request.setPrice(new BigDecimal("10.00"));
        request.setStock(0);

        mockMvc.perform(put("/api/v1/appliances/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void delete_found_returns204() throws Exception {
        when(applianceService.findById(1L)).thenReturn(Optional.of(sampleAppliance()));

        mockMvc.perform(delete("/api/v1/appliances/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(applianceService).delete(1L);
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        when(applianceService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/v1/appliances/99").with(csrf()))
                .andExpect(status().isNotFound());

        verify(applianceService, never()).delete(any());
    }
}