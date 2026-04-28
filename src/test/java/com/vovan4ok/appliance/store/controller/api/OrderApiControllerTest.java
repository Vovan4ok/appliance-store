package com.vovan4ok.appliance.store.controller.api;

import com.vovan4ok.appliance.store.model.Client;
import com.vovan4ok.appliance.store.model.Orders;
import com.vovan4ok.appliance.store.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderApiController.class)
class OrderApiControllerTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(HttpMethod.POST, "/api/v1/orders/**").hasRole("EMPLOYEE")
                            .anyRequest().authenticated()
                    );
            return http.build();
        }
    }

    @Autowired
    MockMvc mockMvc;

    @MockBean
    OrderService orderService;

    @MockBean
    UserDetailsService userDetailsService;

    private Orders orderForClient(String email) {
        Client client = new Client();
        client.setId(1L);
        client.setName("Test Client");
        client.setEmail(email);

        Orders order = new Orders();
        order.setId(1L);
        order.setApproved(false);
        order.setClient(client);
        return order;
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void list_asEmployee_returnsAllOrders() throws Exception {
        when(orderService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(orderForClient("client@test.com"))));

        mockMvc.perform(get("/api/v1/orders").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(orderService).findAll(any(Pageable.class));
        verify(orderService, never()).findByClientEmail(any(), any());
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENT")
    void list_asClient_returnsOwnOrders() throws Exception {
        when(orderService.findByClientEmail(eq("client@test.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(orderForClient("client@test.com"))));

        mockMvc.perform(get("/api/v1/orders").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(orderService).findByClientEmail(eq("client@test.com"), any(Pageable.class));
        verify(orderService, never()).findAll(any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getById_asEmployee_returnsOrder() throws Exception {
        when(orderService.findById(1L)).thenReturn(Optional.of(orderForClient("client@test.com")));

        mockMvc.perform(get("/api/v1/orders/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.approved").value(false));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENT")
    void getById_asClient_ownOrder_returnsOrder() throws Exception {
        when(orderService.findById(1L)).thenReturn(Optional.of(orderForClient("client@test.com")));

        mockMvc.perform(get("/api/v1/orders/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "other@test.com", roles = "CLIENT")
    void getById_asClient_otherOrder_returns404() throws Exception {
        when(orderService.findById(1L)).thenReturn(Optional.of(orderForClient("client@test.com")));

        mockMvc.perform(get("/api/v1/orders/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void approve_returnsApprovedOrder() throws Exception {
        Orders approved = orderForClient("client@test.com");
        approved.setApproved(true);

        doNothing().when(orderService).approve(1L);
        when(orderService.findById(1L)).thenReturn(Optional.of(approved));

        mockMvc.perform(post("/api/v1/orders/1/approve").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved").value(true));
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void approve_asClient_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/orders/1/approve").with(csrf()))
                .andExpect(status().isForbidden());

        verify(orderService, never()).approve(any());
    }
}