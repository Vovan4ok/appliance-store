package com.vovan4ok.appliance.store.controller;

import com.vovan4ok.appliance.store.service.ManufacturerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ManufacturerController.class)
@WithMockUser(roles = "EMPLOYEE")
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ManufacturerService manufacturerService;

    @MockitoBean
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

    @Test
    void wrongHttpMethod_returns405WithErrorViewInsteadOf500() throws Exception {
        // /manufacturers/{id} is GET only
        mockMvc.perform(post("/manufacturers/1").with(csrf()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 405))
                .andExpect(model().attribute("error", "Method Not Allowed"));
    }

    @Test
    void nonNumericId_returns400WithErrorView() throws Exception {
        mockMvc.perform(get("/manufacturers/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 400))
                .andExpect(model().attribute("message", "Invalid value for 'id'"));
    }
}
