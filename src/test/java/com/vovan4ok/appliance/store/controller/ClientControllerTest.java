package com.vovan4ok.appliance.store.controller;

import com.vovan4ok.appliance.store.model.Client;
import com.vovan4ok.appliance.store.service.ClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientController.class)
@WithMockUser(roles = "EMPLOYEE")
class ClientControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ClientService clientService;

    @MockBean
    PasswordEncoder passwordEncoder;

    @MockBean
    UserDetailsService userDetailsService;

    @Test
    void list_returnsClientsView() throws Exception {
        when(clientService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        new Client(1L, "John", "john@mail.com", "hashed", "1234-5678"))));

        mockMvc.perform(get("/clients"))
                .andExpect(status().isOk())
                .andExpect(view().name("client/clients"))
                .andExpect(model().attributeExists("clients", "currentPage", "totalPages"));
    }

    @Test
    void addForm_returnsNewClientView() throws Exception {
        mockMvc.perform(get("/clients/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("client/newClient"))
                .andExpect(model().attributeExists("client"));
    }

    @Test
    void save_validClient_encodesPasswordAndRedirects() throws Exception {
        when(passwordEncoder.encode("Secret@123")).thenReturn("$2a$hashed");
        when(clientService.save(any(Client.class)))
                .thenReturn(new Client(1L, "John", "john@mail.com", "$2a$hashed", "1234-5678"));

        mockMvc.perform(post("/clients/add-client")
                        .param("name", "John")
                        .param("email", "john@mail.com")
                        .param("password", "Secret@123")
                        .param("card", "1234-5678")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clients"));

        verify(passwordEncoder).encode("Secret@123");
        verify(clientService).save(any(Client.class));
    }

    @Test
    void save_blankName_returnsFormWithErrors() throws Exception {
        mockMvc.perform(post("/clients/add-client")
                        .param("name", "")
                        .param("email", "john@mail.com")
                        .param("password", "secret123")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("client/newClient"));

        verify(clientService, never()).save(any());
    }

    @Test
    void save_invalidEmail_returnsFormWithErrors() throws Exception {
        mockMvc.perform(post("/clients/add-client")
                        .param("name", "John")
                        .param("email", "not-an-email")
                        .param("password", "secret123")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("client/newClient"));

        verify(clientService, never()).save(any());
    }

    @Test
    void save_tooShortPassword_returnsFormWithErrors() throws Exception {
        mockMvc.perform(post("/clients/add-client")
                        .param("name", "John")
                        .param("email", "john@mail.com")
                        .param("password", "Ab1!")      // 4 chars — fails min-length
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("client/newClient"));

        verify(clientService, never()).save(any());
    }

    @Test
    void save_weakPassword_noSpecialChar_returnsFormWithErrors() throws Exception {
        mockMvc.perform(post("/clients/add-client")
                        .param("name", "John")
                        .param("email", "john@mail.com")
                        .param("password", "Alllower1") // no special char
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("client/newClient"));

        verify(clientService, never()).save(any());
    }

    @Test
    void save_weakPassword_noUppercase_returnsFormWithErrors() throws Exception {
        mockMvc.perform(post("/clients/add-client")
                        .param("name", "John")
                        .param("email", "john@mail.com")
                        .param("password", "alllower1!") // no uppercase
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("client/newClient"));

        verify(clientService, never()).save(any());
    }

    @Test
    void delete_redirectsToList() throws Exception {
        mockMvc.perform(get("/clients/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clients"));

        verify(clientService).delete(eq(1L));
    }
}
