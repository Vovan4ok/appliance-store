package com.vovan4ok.appliance.store.controller;

import com.vovan4ok.appliance.store.model.Manufacturer;
import com.vovan4ok.appliance.store.service.ManufacturerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ManufacturerController.class)
@WithMockUser(roles = "EMPLOYEE")
class ManufacturerControllerTest {

    @Autowired
    MockMvc mockMvc;

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

    @Test
    void list_returnsManufacturersView() throws Exception {
        when(manufacturerService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mfr(1L, "Samsung"))));

        mockMvc.perform(get("/manufacturers"))
                .andExpect(status().isOk())
                .andExpect(view().name("manufacture/manufacturers"))
                .andExpect(model().attributeExists("manufacturers", "currentPage", "totalPages"));
    }

    @Test
    void list_withSearch_returnsFilteredView() throws Exception {
        when(manufacturerService.findAll(eq("Samsung"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mfr(1L, "Samsung"))));

        mockMvc.perform(get("/manufacturers").param("search", "Samsung"))
                .andExpect(status().isOk())
                .andExpect(view().name("manufacture/manufacturers"))
                .andExpect(model().attribute("filterSearch", "Samsung"));
    }

    @Test
    void detail_found_returnsDetailView() throws Exception {
        when(manufacturerService.findById(1L))
                .thenReturn(Optional.of(mfr(1L, "Samsung")));

        mockMvc.perform(get("/manufacturers/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("manufacture/manufacturerDetail"))
                .andExpect(model().attributeExists("manufacturer"));
    }

    @Test
    void detail_notFound_showsErrorPage() throws Exception {
        when(manufacturerService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/manufacturers/99"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"));
    }

    @Test
    void addForm_returnsNewManufacturerView() throws Exception {
        mockMvc.perform(get("/manufacturers/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("manufacture/newManufacturer"))
                .andExpect(model().attributeExists("manufacturer"));
    }

    @Test
    void save_validName_redirectsToList() throws Exception {
        mockMvc.perform(multipart("/manufacturers/add-manufacturer")
                        .param("name", "Bosch")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manufacturers"));

        verify(manufacturerService).save(any(Manufacturer.class));
    }

    @Test
    void save_blankName_returnsFormWithErrors() throws Exception {
        mockMvc.perform(multipart("/manufacturers/add-manufacturer")
                        .param("name", "")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("manufacture/newManufacturer"));

        verify(manufacturerService, never()).save(any());
    }

    @Test
    void editForm_found_returnsEditView() throws Exception {
        when(manufacturerService.findById(1L))
                .thenReturn(Optional.of(mfr(1L, "Samsung")));

        mockMvc.perform(get("/manufacturers/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("manufacture/editManufacturer"))
                .andExpect(model().attributeExists("manufacturer", "manufacturerId"));
    }

    @Test
    void editForm_notFound_showsErrorPage() throws Exception {
        when(manufacturerService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/manufacturers/99/edit"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"));
    }

    @Test
    void update_validName_redirectsToDetail() throws Exception {
        when(manufacturerService.findById(1L))
                .thenReturn(Optional.of(mfr(1L, "Samsung")));

        mockMvc.perform(multipart("/manufacturers/1/update")
                        .param("name", "Samsung Updated")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manufacturers/1"));
    }

    @Test
    void update_blankName_returnsFormWithErrors() throws Exception {
        mockMvc.perform(multipart("/manufacturers/1/update")
                        .param("name", "")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("manufacture/editManufacturer"));

        verify(manufacturerService, never()).save(any());
    }

    @Test
    void delete_redirectsToList() throws Exception {
        mockMvc.perform(get("/manufacturers/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manufacturers"));

        verify(manufacturerService).delete(1L);
    }

    @Test
    void delete_dataIntegrityViolation_showsConflictErrorPage() throws Exception {
        doThrow(new DataIntegrityViolationException("FK constraint"))
                .when(manufacturerService).delete(eq(1L));

        mockMvc.perform(get("/manufacturers/1/delete"))
                .andExpect(status().isConflict())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 409));
    }
}