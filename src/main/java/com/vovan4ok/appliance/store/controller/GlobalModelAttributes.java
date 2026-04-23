package com.vovan4ok.appliance.store.controller;

import com.vovan4ok.appliance.store.service.ClientService;
import com.vovan4ok.appliance.store.service.EmployeeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    // required = false keeps @WebMvcTest slices working without mocking these services
    @Autowired(required = false)
    private ClientService clientService;

    @Autowired(required = false)
    private EmployeeService employeeService;

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("currentUserAvatarPath")
    public String currentUserAvatarPath(Authentication auth) {
        if (clientService == null || employeeService == null) return null;
        if (auth == null || !auth.isAuthenticated()) return null;
        String email = auth.getName();
        return employeeService.findByEmail(email)
                .map(e -> e.getAvatarPath())
                .orElseGet(() -> clientService.findByEmail(email)
                        .map(c -> c.getAvatarPath())
                        .orElse(null));
    }

    @ModelAttribute("currentUserDisplayName")
    public String currentUserDisplayName(Authentication auth) {
        if (clientService == null || employeeService == null) return null;
        if (auth == null || !auth.isAuthenticated()) return null;
        String email = auth.getName();
        return employeeService.findByEmail(email)
                .map(e -> e.getName())
                .orElseGet(() -> clientService.findByEmail(email)
                        .map(c -> c.getName())
                        .orElse(email));
    }
}