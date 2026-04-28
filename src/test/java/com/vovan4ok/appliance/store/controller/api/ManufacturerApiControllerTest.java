package com.vovan4ok.appliance.store.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vovan4ok.appliance.store.model.Manufacturer;
import com.vovan4ok.appliance.store.model.dto.api.ManufacturerRequest;
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

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ManufacturerApiController.class)
@WithMockUser(roles = "EMPLOYEE")
class ManufacturerApiControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    ManufacturerService manufacturerService;

    @MockBean
    UserDetailsService userDetailsService;

    @Test
    void list_returnsPageOfManufacturers() throws Exception {
        when(manufacturerService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new Manufacturer(1L, "Samsung"))));

        mockMvc.perform(get("/api/v1/manufacturers").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Samsung"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getById_found_returnsManufacturer() throws Exception {
        when(manufacturerService.findById(1L)).thenReturn(Optional.of(new Manufacturer(1L, "Samsung")));

        mockMvc.perform(get("/api/v1/manufacturers/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Samsung"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(manufacturerService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/manufacturers/99").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(manufacturerService.save(any())).thenReturn(new Manufacturer(2L, "Bosch"));

        ManufacturerRequest request = new ManufacturerRequest();
        request.setName("Bosch");

        mockMvc.perform(post("/api/v1/manufacturers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Bosch"));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        ManufacturerRequest request = new ManufacturerRequest();
        request.setName("");

        mockMvc.perform(post("/api/v1/manufacturers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(manufacturerService, never()).save(any());
    }

    @Test
    void update_found_returnsUpdated() throws Exception {
        when(manufacturerService.findById(1L)).thenReturn(Optional.of(new Manufacturer(1L, "Samsung")));
        when(manufacturerService.save(any())).thenReturn(new Manufacturer(1L, "Samsung Updated"));

        ManufacturerRequest request = new ManufacturerRequest();
        request.setName("Samsung Updated");

        mockMvc.perform(put("/api/v1/manufacturers/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Samsung Updated"));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        when(manufacturerService.findById(99L)).thenReturn(Optional.empty());

        ManufacturerRequest request = new ManufacturerRequest();
        request.setName("Brand");

        mockMvc.perform(put("/api/v1/manufacturers/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_found_returns204() throws Exception {
        when(manufacturerService.findById(1L)).thenReturn(Optional.of(new Manufacturer(1L, "Samsung")));

        mockMvc.perform(delete("/api/v1/manufacturers/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(manufacturerService).delete(1L);
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        when(manufacturerService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/v1/manufacturers/99").with(csrf()))
                .andExpect(status().isNotFound());

        verify(manufacturerService, never()).delete(any());
    }
}