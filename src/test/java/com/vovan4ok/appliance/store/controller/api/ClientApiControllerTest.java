package com.vovan4ok.appliance.store.controller.api;

import com.vovan4ok.appliance.store.model.Client;
import com.vovan4ok.appliance.store.service.ClientService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientApiController.class)
@WithMockUser(roles = "EMPLOYEE")
class ClientApiControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ClientService clientService;

    @MockBean
    UserDetailsService userDetailsService;

    private Client sampleClient() {
        Client c = new Client();
        c.setId(1L);
        c.setName("Mercury");
        c.setEmail("mercury@test.com");
        c.setCard("1234-5678");
        return c;
    }

    @Test
    void list_returnsPageOfClients() throws Exception {
        when(clientService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleClient())));

        mockMvc.perform(get("/api/v1/clients").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Mercury"))
                .andExpect(jsonPath("$.content[0].email").value("mercury@test.com"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getById_found_returnsClient() throws Exception {
        when(clientService.findById(1L)).thenReturn(Optional.of(sampleClient()));

        mockMvc.perform(get("/api/v1/clients/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Mercury"))
                .andExpect(jsonPath("$.card").value("1234-5678"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(clientService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/clients/99").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}