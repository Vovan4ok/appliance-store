package com.vovan4ok.appliance.store.controller;

import com.vovan4ok.appliance.store.model.Appliance;
import com.vovan4ok.appliance.store.model.Category;
import com.vovan4ok.appliance.store.model.Manufacturer;
import com.vovan4ok.appliance.store.model.PowerType;
import com.vovan4ok.appliance.store.service.ApplianceService;
import com.vovan4ok.appliance.store.service.ManufacturerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApplianceController.class)
@WithMockUser(roles = "EMPLOYEE")
class ApplianceControllerTest {

    @Autowired
    MockMvc mockMvc;

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

    private Appliance buildAppliance() {
        return new Appliance(1L, "Fridge", Category.BIG, "RB37", mfr(1L, "Samsung"),
                PowerType.AC220, "A++", "Big fridge", 100, BigDecimal.valueOf(500), 10);
    }

    @Test
    void list_returnsAppliancesView() throws Exception {
        when(applianceService.findAll(any(), any(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildAppliance())));
        when(manufacturerService.findAll()).thenReturn(List.of(mfr(1L, "Samsung")));

        mockMvc.perform(get("/appliances"))
                .andExpect(status().isOk())
                .andExpect(view().name("appliance/appliances"))
                .andExpect(model().attributeExists("appliances", "currentPage", "totalPages",
                        "categories", "powerTypes", "manufacturers"));
    }

    @Test
    void addForm_returnsNewApplianceViewWithLookups() throws Exception {
        when(manufacturerService.findAll()).thenReturn(List.of(mfr(1L, "Samsung")));

        mockMvc.perform(get("/appliances/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("appliance/newAppliance"))
                .andExpect(model().attributeExists("appliance", "manufacturers", "categories", "powerTypes"));
    }

    @Test
    void save_validAppliance_redirectsToList() throws Exception {
        when(manufacturerService.findById(1L)).thenReturn(Optional.of(mfr(1L, "Samsung")));
        when(applianceService.save(any(Appliance.class))).thenReturn(buildAppliance());

        mockMvc.perform(post("/appliances/add")
                        .param("name", "Fridge")
                        .param("category", "BIG")
                        .param("model", "RB37")
                        .param("manufacturerId", "1")
                        .param("powerType", "AC220")
                        .param("characteristic", "A++")
                        .param("description", "Big fridge")
                        .param("power", "100")
                        .param("price", "500.00")
                        .param("stock", "10")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/appliances"));

        verify(applianceService).save(any(Appliance.class));
    }

    @Test
    void save_missingName_returnsFormWithErrors() throws Exception {
        when(manufacturerService.findAll()).thenReturn(List.of(mfr(1L, "Samsung")));

        mockMvc.perform(post("/appliances/add")
                        .param("name", "")
                        .param("category", "BIG")
                        .param("model", "RB37")
                        .param("manufacturerId", "1")
                        .param("powerType", "AC220")
                        .param("characteristic", "A++")
                        .param("description", "Big fridge")
                        .param("power", "100")
                        .param("price", "500.00")
                        .param("stock", "10")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("appliance/newAppliance"));

        verify(applianceService, never()).save(any());
    }

    @Test
    void editForm_found_returnsEditView() throws Exception {
        when(applianceService.findById(1L)).thenReturn(Optional.of(buildAppliance()));
        when(manufacturerService.findAll()).thenReturn(List.of(mfr(1L, "Samsung")));

        mockMvc.perform(get("/appliances/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("appliance/editAppliance"))
                .andExpect(model().attributeExists("appliance", "applianceId", "manufacturers", "categories", "powerTypes"));
    }

    @Test
    void editForm_notFound_showsErrorPage() throws Exception {
        when(applianceService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/appliances/99/edit"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"));
    }

    @Test
    void update_validAppliance_redirectsToList() throws Exception {
        when(applianceService.findById(1L)).thenReturn(Optional.of(buildAppliance()));
        when(manufacturerService.findById(1L)).thenReturn(Optional.of(mfr(1L, "Samsung")));
        when(applianceService.save(any(Appliance.class))).thenReturn(buildAppliance());

        mockMvc.perform(post("/appliances/1/update")
                        .param("name", "Fridge Updated")
                        .param("category", "BIG")
                        .param("model", "RB37")
                        .param("manufacturerId", "1")
                        .param("powerType", "AC220")
                        .param("characteristic", "A++")
                        .param("description", "Big fridge")
                        .param("power", "100")
                        .param("price", "500.00")
                        .param("stock", "10")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/appliances"));
    }

    @Test
    void update_invalidData_returnsFormWithErrors() throws Exception {
        when(manufacturerService.findAll()).thenReturn(List.of(mfr(1L, "Samsung")));

        mockMvc.perform(post("/appliances/1/update")
                        .param("name", "")
                        .param("category", "BIG")
                        .param("model", "RB37")
                        .param("manufacturerId", "1")
                        .param("powerType", "AC220")
                        .param("characteristic", "A++")
                        .param("description", "Big fridge")
                        .param("power", "100")
                        .param("price", "500.00")
                        .param("stock", "10")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("appliance/editAppliance"));
    }

    @Test
    void delete_redirectsToList() throws Exception {
        mockMvc.perform(get("/appliances/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/appliances"));

        verify(applianceService).delete(eq(1L));
    }
}
