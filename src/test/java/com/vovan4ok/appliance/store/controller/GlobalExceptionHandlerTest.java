package com.vovan4ok.appliance.store.controller;

import com.vovan4ok.appliance.store.service.ManufacturerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ManufacturerController.class)
@WithMockUser(roles = "EMPLOYEE")
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ManufacturerService manufacturerService;

    @MockBean
    UserDetailsService userDetailsService;

    @Test
    void dataIntegrityViolation_returns409WithErrorView() throws Exception {
        doThrow(new DataIntegrityViolationException("FK constraint violated"))
                .when(manufacturerService).delete(eq(1L));

        mockMvc.perform(get("/manufacturers/1/delete"))
                .andExpect(status().isConflict())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 409))
                .andExpect(model().attribute("error", "Conflict"));
    }

    @Test
    void illegalArgumentException_returns404WithErrorView() throws Exception {
        when(manufacturerService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/manufacturers/99/edit"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 404))
                .andExpect(model().attribute("error", "Not Found"));
    }

    @Test
    void genericException_returns500WithErrorView() throws Exception {
        doThrow(new RuntimeException("Unexpected failure"))
                .when(manufacturerService).findAll(any(Pageable.class));

        mockMvc.perform(get("/manufacturers"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 500))
                .andExpect(model().attribute("error", "Internal Server Error"));
    }
}
