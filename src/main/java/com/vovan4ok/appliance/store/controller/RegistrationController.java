package com.vovan4ok.appliance.store.controller;

import com.vovan4ok.appliance.store.model.Client;
import com.vovan4ok.appliance.store.model.dto.ClientDto;
import com.vovan4ok.appliance.store.service.ClientService;
import com.vovan4ok.appliance.store.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/register")
@RequiredArgsConstructor
public class RegistrationController {

    private final ClientService clientService;
    private final EmployeeService employeeService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public String showForm(Model model) {
        model.addAttribute("client", new ClientDto());
        return "register";
    }

    @PostMapping
    public String register(@Valid @ModelAttribute("client") ClientDto dto,
                           BindingResult result,
                           Model model) {
        if (result.hasErrors()) {
            return "register";
        }

        boolean emailTaken = clientService.findByEmail(dto.getEmail()).isPresent()
                || employeeService.findByEmail(dto.getEmail()).isPresent();

        if (emailTaken) {
            model.addAttribute("emailError", true);
            return "register";
        }

        Client client = new Client(null, dto.getName(), dto.getEmail(),
                passwordEncoder.encode(dto.getPassword()), dto.getCard());
        clientService.save(client);
        log.info("New client registered: {}", dto.getEmail());

        return "redirect:/login?registered";
    }
}
