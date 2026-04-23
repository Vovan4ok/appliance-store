package com.vovan4ok.appliance.store.controller;

import com.vovan4ok.appliance.store.model.Client;
import com.vovan4ok.appliance.store.model.Employee;
import com.vovan4ok.appliance.store.model.Orders;
import com.vovan4ok.appliance.store.model.Manufacturer;
import com.vovan4ok.appliance.store.service.ApplianceService;
import com.vovan4ok.appliance.store.service.ClientService;
import com.vovan4ok.appliance.store.service.EmployeeService;
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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrdersController.class)
@WithMockUser
class OrdersControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    OrderService orderService;

    @MockBean
    ClientService clientService;

    @MockBean
    EmployeeService employeeService;

    @MockBean
    ApplianceService applianceService;

    @MockBean
    ManufacturerService manufacturerService;

    @MockBean
    UserDetailsService userDetailsService;

    private Orders buildOrder(Long id) {
        Orders order = new Orders();
        order.setId(id);
        order.setApproved(false);
        order.setOrderRowSet(new HashSet<>());
        return order;
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void list_returnsOrdersView() throws Exception {
        when(orderService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildOrder(1L))));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("order/orders"))
                .andExpect(model().attributeExists("orders", "currentPage", "totalPages", "isEmployee"));
    }

    @Test
    @WithMockUser(username = "client@mail.com", roles = "CLIENT")
    void list_asClient_returnsOnlyClientOrders() throws Exception {
        when(orderService.findByClientEmail(eq("client@mail.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildOrder(2L))));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("order/orders"))
                .andExpect(model().attributeExists("orders", "currentPage", "totalPages", "isEmployee"));
    }

    @Test
    void addForm_returnsNewOrderViewWithLookups() throws Exception {
        when(clientService.findAll()).thenReturn(List.of(
                new Client(1L, "John", "john@mail.com", "hashed", "1234")));
        when(employeeService.findAll()).thenReturn(List.of(
                new Employee(1L, "Alice", "alice@mail.com", "hashed", "sales")));

        mockMvc.perform(get("/orders/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("order/newOrder"))
                .andExpect(model().attributeExists("order", "clients", "employees"));
    }

    @Test
    void saveOrder_redirectsToEditPage() throws Exception {
        Client client = new Client(1L, "John", "john@mail.com", "hashed", "1234");
        Employee employee = new Employee(2L, "Alice", "alice@mail.com", "hashed", "sales");
        Orders saved = buildOrder(5L);
        saved.setClient(client);

        when(clientService.findById(1L)).thenReturn(Optional.of(client));
        when(employeeService.findById(2L)).thenReturn(Optional.of(employee));
        when(orderService.save(any(Orders.class))).thenReturn(saved);

        mockMvc.perform(post("/orders/add-order")
                        .param("clientId", "1")
                        .param("employeeId", "2")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/5/edit"));
    }

    @Test
    void editOrder_found_returnsEditView() throws Exception {
        Orders order = buildOrder(1L);
        when(orderService.findById(1L)).thenReturn(Optional.of(order));

        mockMvc.perform(get("/orders/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("order/editOrder"))
                .andExpect(model().attributeExists("order", "rows"));
    }

    @Test
    void editOrder_notFound_showsErrorPage() throws Exception {
        when(orderService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/orders/99/edit"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"));
    }

    @Test
    void choiceAppliance_returnsChoiceView() throws Exception {
        when(applianceService.findAll(any(), any(), any(), any(), any(), any(), anyBoolean(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(manufacturerService.findAll()).thenReturn(List.of(new Manufacturer(1L, "Samsung")));

        mockMvc.perform(get("/orders/1/choice-appliance"))
                .andExpect(status().isOk())
                .andExpect(view().name("order/choiceAppliance"))
                .andExpect(model().attributeExists("appliances", "ordersId",
                        "currentPage", "totalPages", "categories", "powerTypes", "manufacturers"));
    }

    @Test
    void addIntoOrder_redirectsToEditPage() throws Exception {
        mockMvc.perform(post("/orders/1/add-into-order")
                        .param("applianceId", "2")
                        .param("numbers", "3")
                        .param("price", "99.99")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/1/edit"));

        verify(orderService).addOrderRow(eq(1L), eq(2L), eq(3L), any());
    }

    @Test
    void updateRow_redirectsToEditPage() throws Exception {
        mockMvc.perform(post("/orders/1/rows/3/update")
                        .param("number", "5")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/1/edit"));

        verify(orderService).updateOrderRowQuantity(eq(3L), eq(5L));
    }

    @Test
    void removeRow_orderStillExists_redirectsToEditPage() throws Exception {
        when(orderService.findById(1L)).thenReturn(Optional.of(buildOrder(1L)));

        mockMvc.perform(get("/orders/1/rows/3/remove"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/1/edit"));

        verify(orderService).removeOrderRow(eq(3L));
    }

    @Test
    void removeRow_lastRow_redirectsToList() throws Exception {
        when(orderService.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/orders/1/rows/3/remove"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"));

        verify(orderService).removeOrderRow(eq(3L));
    }

    @Test
    void delete_redirectsToList() throws Exception {
        mockMvc.perform(get("/orders/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"));

        verify(orderService).delete(eq(1L));
    }

    @Test
    void approve_redirectsToList() throws Exception {
        mockMvc.perform(get("/orders/1/approved"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"));

        verify(orderService).approve(eq(1L));
    }

    @Test
    void unapprove_redirectsToList() throws Exception {
        mockMvc.perform(get("/orders/1/unapproved"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"));

        verify(orderService).unapprove(eq(1L));
    }
}
