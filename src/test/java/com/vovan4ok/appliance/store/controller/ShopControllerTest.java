package com.vovan4ok.appliance.store.controller;

import com.vovan4ok.appliance.store.model.Appliance;
import com.vovan4ok.appliance.store.model.Client;
import com.vovan4ok.appliance.store.model.Manufacturer;
import com.vovan4ok.appliance.store.model.Orders;
import com.vovan4ok.appliance.store.model.OrderRow;
import com.vovan4ok.appliance.store.service.ApplianceService;
import com.vovan4ok.appliance.store.service.ClientService;
import com.vovan4ok.appliance.store.service.ManufacturerService;
import com.vovan4ok.appliance.store.service.OrderService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShopController.class)
@WithMockUser(username = "client@mail.com", roles = "CLIENT")
class ShopControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ApplianceService applianceService;

    @MockBean
    OrderService orderService;

    @MockBean
    ClientService clientService;

    @MockBean
    ManufacturerService manufacturerService;

    @MockBean
    UserDetailsService userDetailsService;

    private Orders buildPendingOrder(Long id) {
        Orders order = new Orders();
        order.setId(id);
        order.setApproved(false);
        order.setOrderRowSet(new HashSet<>());
        return order;
    }

    @Test
    void catalog_returnsShopViewWithAttributes() throws Exception {
        when(applianceService.findAll(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(manufacturerService.findAll()).thenReturn(List.of(new Manufacturer(1L, "Samsung")));
        when(orderService.findPendingByClientEmail(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/shop"))
                .andExpect(status().isOk())
                .andExpect(view().name("shop/shop"))
                .andExpect(model().attributeExists(
                        "appliances", "currentPage", "totalPages",
                        "categories", "powerTypes", "manufacturers",
                        "sortBy", "sortDir"));
    }

    @Test
    void catalog_withCartItems_populatesCartCount() throws Exception {
        Orders order = buildPendingOrder(1L);
        order.getOrderRowSet().add(new OrderRow());
        when(applianceService.findAll(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(manufacturerService.findAll()).thenReturn(List.of());
        when(orderService.findPendingByClientEmail("client@mail.com")).thenReturn(Optional.of(order));

        mockMvc.perform(get("/shop"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("cartCount", 1));
    }

    @Test
    void addToCart_existingPendingOrder_addsRowAndRedirects() throws Exception {
        Appliance appliance = new Appliance();
        appliance.setId(2L);
        appliance.setPrice(BigDecimal.valueOf(99));
        Orders order = buildPendingOrder(1L);

        when(applianceService.findById(2L)).thenReturn(Optional.of(appliance));
        when(orderService.findPendingByClientEmail("client@mail.com")).thenReturn(Optional.of(order));

        mockMvc.perform(post("/shop/add-to-cart")
                        .param("applianceId", "2")
                        .param("quantity", "1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shop"));

        verify(orderService).addOrderRow(eq(1L), eq(2L), eq(1L), eq(BigDecimal.valueOf(99)));
    }

    @Test
    void addToCart_noPendingOrder_createsNewOrderThenAddsRow() throws Exception {
        Appliance appliance = new Appliance();
        appliance.setId(2L);
        appliance.setPrice(BigDecimal.valueOf(99));
        Client client = new Client(3L, "Test", "client@mail.com", "hash", "1111");
        Orders newOrder = buildPendingOrder(10L);

        when(applianceService.findById(2L)).thenReturn(Optional.of(appliance));
        when(orderService.findPendingByClientEmail("client@mail.com")).thenReturn(Optional.empty());
        when(clientService.findByEmail("client@mail.com")).thenReturn(Optional.of(client));
        when(orderService.save(any(Orders.class))).thenReturn(newOrder);

        mockMvc.perform(post("/shop/add-to-cart")
                        .param("applianceId", "2")
                        .param("quantity", "2")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shop"));

        verify(orderService).save(any(Orders.class));
        verify(orderService).addOrderRow(eq(10L), eq(2L), eq(2L), eq(BigDecimal.valueOf(99)));
    }

    @Test
    void addToCart_applianceNotFound_showsErrorPage() throws Exception {
        when(applianceService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/shop/add-to-cart")
                        .param("applianceId", "99")
                        .param("quantity", "1")
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"));
    }

    @Test
    void cart_returnsCartViewWithOrder() throws Exception {
        Orders order = buildPendingOrder(1L);
        when(orderService.findPendingByClientEmail("client@mail.com")).thenReturn(Optional.of(order));

        mockMvc.perform(get("/shop/cart"))
                .andExpect(status().isOk())
                .andExpect(view().name("shop/cart"))
                .andExpect(model().attributeExists("order"));
    }

    @Test
    void cart_noPendingOrder_returnsCartViewWithNullOrder() throws Exception {
        when(orderService.findPendingByClientEmail("client@mail.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/shop/cart"))
                .andExpect(status().isOk())
                .andExpect(view().name("shop/cart"));
    }

    @Test
    void updateCartRow_redirectsToCart() throws Exception {
        mockMvc.perform(post("/shop/cart/rows/3/update")
                        .param("number", "5")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shop/cart"));

        verify(orderService).updateOrderRowQuantity(eq(3L), eq(5L));
    }

    @Test
    void removeCartRow_redirectsToCart() throws Exception {
        mockMvc.perform(post("/shop/cart/rows/3/remove")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shop/cart"));

        verify(orderService).removeOrderRow(eq(3L));
    }

    @Test
    void checkout_submitsCartAndRedirectsToOrder() throws Exception {
        mockMvc.perform(post("/shop/checkout")
                        .param("orderId", "1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/1/edit"));

        verify(orderService).submitCart(eq(1L));
    }

    @Test
    void cancelOrder_redirectsToShop() throws Exception {
        mockMvc.perform(post("/shop/cancel")
                        .param("orderId", "1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shop"));

        verify(orderService).delete(eq(1L));
    }
}
