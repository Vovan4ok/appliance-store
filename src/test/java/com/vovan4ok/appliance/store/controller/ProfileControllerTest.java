package com.vovan4ok.appliance.store.controller;

import com.vovan4ok.appliance.store.model.Client;
import com.vovan4ok.appliance.store.model.Employee;
import com.vovan4ok.appliance.store.service.ClientService;
import com.vovan4ok.appliance.store.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
class ProfileControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ClientService clientService;
    @MockBean
    EmployeeService employeeService;
    @MockBean
    PasswordEncoder passwordEncoder;
    @MockBean
    UserDetailsService userDetailsService;

    private Client client;
    private Employee employee;

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setId(1L);
        client.setName("Mercury");
        client.setEmail("mercury@example.com");
        client.setPassword("$2a$10$hashedpassword");
        client.setCard("1234-5678");

        employee = new Employee();
        employee.setId(2L);
        employee.setName("Alice");
        employee.setEmail("alice@example.com");
        employee.setPassword("$2a$10$hashedpassword");
        employee.setDepartment("Sales");
    }

    // ── GET /profile ──────────────────────────────────────────────

    @Test
    @WithMockUser(username = "mercury@example.com", roles = "CLIENT")
    void profile_client_returnsProfileView() throws Exception {
        when(employeeService.findByEmail("mercury@example.com")).thenReturn(Optional.empty());
        when(clientService.findByEmail("mercury@example.com")).thenReturn(Optional.of(client));

        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attributeExists("profile", "passwordForm", "user"))
                .andExpect(model().attribute("isEmployee", false));
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = "EMPLOYEE")
    void profile_employee_returnsProfileViewWithIsEmployeeTrue() throws Exception {
        when(employeeService.findByEmail("alice@example.com")).thenReturn(Optional.of(employee));

        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attribute("isEmployee", true));
    }

    // ── POST /profile/edit ────────────────────────────────────────

    @Test
    @WithMockUser(username = "mercury@example.com", roles = "CLIENT")
    void editProfile_validData_redirectsWithUpdated() throws Exception {
        when(employeeService.findByEmail("mercury@example.com")).thenReturn(Optional.empty());
        when(clientService.findByEmail("mercury@example.com")).thenReturn(Optional.of(client));
        when(clientService.save(any())).thenReturn(client);

        mockMvc.perform(post("/profile/edit")
                        .param("name", "Mercury Updated")
                        .param("phone", "+380501234567")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?updated"));

        verify(clientService).save(any());
    }

    @Test
    @WithMockUser(username = "mercury@example.com", roles = "CLIENT")
    void editProfile_blankName_returnsFormWithErrors() throws Exception {
        when(employeeService.findByEmail("mercury@example.com")).thenReturn(Optional.empty());
        when(clientService.findByEmail("mercury@example.com")).thenReturn(Optional.of(client));

        mockMvc.perform(post("/profile/edit")
                        .param("name", "")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attributeHasFieldErrors("profile", "name"));

        verify(clientService, never()).save(any());
    }

    @Test
    @WithMockUser(username = "mercury@example.com", roles = "CLIENT")
    void editProfile_futureDateOfBirth_returnsFormWithErrors() throws Exception {
        when(employeeService.findByEmail("mercury@example.com")).thenReturn(Optional.empty());
        when(clientService.findByEmail("mercury@example.com")).thenReturn(Optional.of(client));

        mockMvc.perform(post("/profile/edit")
                        .param("name", "Mercury")
                        .param("dateOfBirth", "2099-01-01")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attributeHasFieldErrors("profile", "dateOfBirth"));
    }

    // ── POST /profile/change-password ────────────────────────────

    @Test
    @WithMockUser(username = "mercury@example.com", roles = "CLIENT")
    void changePassword_valid_redirectsWithPasswordChanged() throws Exception {
        when(employeeService.findByEmail("mercury@example.com")).thenReturn(Optional.empty());
        when(clientService.findByEmail("mercury@example.com")).thenReturn(Optional.of(client));
        when(passwordEncoder.matches("OldPass@1!", client.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("NewPass@1!")).thenReturn("$2a$10$newHash");
        when(clientService.save(any())).thenReturn(client);

        mockMvc.perform(post("/profile/change-password")
                        .param("oldPassword", "OldPass@1!")
                        .param("newPassword", "NewPass@1!")
                        .param("confirmPassword", "NewPass@1!")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?passwordChanged"));
    }

    @Test
    @WithMockUser(username = "mercury@example.com", roles = "CLIENT")
    void changePassword_wrongOldPassword_returnsFormWithError() throws Exception {
        when(employeeService.findByEmail("mercury@example.com")).thenReturn(Optional.empty());
        when(clientService.findByEmail("mercury@example.com")).thenReturn(Optional.of(client));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        mockMvc.perform(post("/profile/change-password")
                        .param("oldPassword", "WrongPass@1!")
                        .param("newPassword", "NewPass@1!")
                        .param("confirmPassword", "NewPass@1!")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attribute("wrongPassword", true));
    }

    @Test
    @WithMockUser(username = "mercury@example.com", roles = "CLIENT")
    void changePassword_mismatch_returnsFormWithError() throws Exception {
        when(employeeService.findByEmail("mercury@example.com")).thenReturn(Optional.empty());
        when(clientService.findByEmail("mercury@example.com")).thenReturn(Optional.of(client));
        when(passwordEncoder.matches("OldPass@1!", client.getPassword())).thenReturn(true);

        mockMvc.perform(post("/profile/change-password")
                        .param("oldPassword", "OldPass@1!")
                        .param("newPassword", "NewPass@1!")
                        .param("confirmPassword", "DifferentPass@1!")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attribute("passwordMismatch", true));
    }

    @Test
    @WithMockUser(username = "mercury@example.com", roles = "CLIENT")
    void changePassword_invalidNewPassword_returnsFormWithErrors() throws Exception {
        when(employeeService.findByEmail("mercury@example.com")).thenReturn(Optional.empty());
        when(clientService.findByEmail("mercury@example.com")).thenReturn(Optional.of(client));

        mockMvc.perform(post("/profile/change-password")
                        .param("oldPassword", "OldPass@1!")
                        .param("newPassword", "weak")
                        .param("confirmPassword", "weak")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attributeHasFieldErrors("passwordForm", "newPassword"));
    }

    // ── POST /profile/avatar ──────────────────────────────────────

    @Test
    @WithMockUser(username = "mercury@example.com", roles = "CLIENT")
    void uploadAvatar_validJpeg_redirectsWithAvatarUpdated() throws Exception {
        when(employeeService.findByEmail("mercury@example.com")).thenReturn(Optional.empty());
        when(clientService.findByEmail("mercury@example.com")).thenReturn(Optional.of(client));

        MockMultipartFile file = new MockMultipartFile(
                "avatar", "photo.jpg", "image/jpeg", new byte[1024]);

        mockMvc.perform(multipart("/profile/avatar").file(file).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/profile?avatarUpdated"));

        verify(clientService).updateAvatar(eq(1L), anyString());
    }

    @Test
    @WithMockUser(username = "mercury@example.com", roles = "CLIENT")
    void uploadAvatar_invalidType_redirectsWithAvatarError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "avatar", "doc.pdf", "application/pdf", new byte[1024]);

        mockMvc.perform(multipart("/profile/avatar").file(file).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?avatarError"));
    }

    @Test
    @WithMockUser(username = "mercury@example.com", roles = "CLIENT")
    void uploadAvatar_emptyFile_redirectsWithAvatarError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "avatar", "empty.jpg", "image/jpeg", new byte[0]);

        mockMvc.perform(multipart("/profile/avatar").file(file).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?avatarError"));
    }
}