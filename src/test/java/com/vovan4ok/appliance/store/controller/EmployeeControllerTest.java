package com.vovan4ok.appliance.store.controller;

import com.vovan4ok.appliance.store.model.Employee;
import com.vovan4ok.appliance.store.service.EmployeeService;
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

@WebMvcTest(EmployeeController.class)
@WithMockUser(roles = "EMPLOYEE")
class EmployeeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    EmployeeService employeeService;

    @MockBean
    PasswordEncoder passwordEncoder;

    @MockBean
    UserDetailsService userDetailsService;

    @Test
    void list_returnsEmployeesView() throws Exception {
        when(employeeService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        new Employee(1L, "Alice", "alice@mail.com", "hashed", "sales"))));

        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee/employees"))
                .andExpect(model().attributeExists("employees", "currentPage", "totalPages"));
    }

    @Test
    void addForm_returnsNewEmployeeView() throws Exception {
        mockMvc.perform(get("/employees/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("employee/newEmployee"))
                .andExpect(model().attributeExists("employee"));
    }

    @Test
    void save_validEmployee_encodesPasswordAndRedirects() throws Exception {
        when(passwordEncoder.encode("Secret@123")).thenReturn("$2a$hashed");
        when(employeeService.save(any(Employee.class)))
                .thenReturn(new Employee(1L, "Alice", "alice@mail.com", "$2a$hashed", "sales"));

        mockMvc.perform(post("/employees/add-employee")
                        .param("name", "Alice")
                        .param("email", "alice@mail.com")
                        .param("password", "Secret@123")
                        .param("department", "sales")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employees"));

        verify(passwordEncoder).encode("Secret@123");
        verify(employeeService).save(any(Employee.class));
    }

    @Test
    void save_blankName_returnsFormWithErrors() throws Exception {
        mockMvc.perform(post("/employees/add-employee")
                        .param("name", "")
                        .param("email", "alice@mail.com")
                        .param("password", "secret123")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("employee/newEmployee"));

        verify(employeeService, never()).save(any());
    }

    @Test
    void save_invalidEmail_returnsFormWithErrors() throws Exception {
        mockMvc.perform(post("/employees/add-employee")
                        .param("name", "Alice")
                        .param("email", "not-an-email")
                        .param("password", "secret123")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("employee/newEmployee"));

        verify(employeeService, never()).save(any());
    }

    @Test
    void save_tooShortPassword_returnsFormWithErrors() throws Exception {
        mockMvc.perform(post("/employees/add-employee")
                        .param("name", "Alice")
                        .param("email", "alice@mail.com")
                        .param("password", "Ab1!")     // 4 chars — fails min-length
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("employee/newEmployee"));

        verify(employeeService, never()).save(any());
    }

    @Test
    void save_weakPassword_noSpecialChar_returnsFormWithErrors() throws Exception {
        mockMvc.perform(post("/employees/add-employee")
                        .param("name", "Alice")
                        .param("email", "alice@mail.com")
                        .param("password", "Alllower1")  // no special char
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("employee/newEmployee"));

        verify(employeeService, never()).save(any());
    }

    @Test
    void save_weakPassword_noUppercase_returnsFormWithErrors() throws Exception {
        mockMvc.perform(post("/employees/add-employee")
                        .param("name", "Alice")
                        .param("email", "alice@mail.com")
                        .param("password", "alllower1!")  // no uppercase
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("employee/newEmployee"));

        verify(employeeService, never()).save(any());
    }

    @Test
    void delete_redirectsToList() throws Exception {
        mockMvc.perform(get("/employees/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employees"));

        verify(employeeService).delete(eq(1L));
    }
}
