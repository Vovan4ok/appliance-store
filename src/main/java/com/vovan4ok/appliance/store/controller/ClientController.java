package com.vovan4ok.appliance.store.controller;

import com.vovan4ok.appliance.store.model.Client;
import com.vovan4ok.appliance.store.model.dto.ClientDto;
import com.vovan4ok.appliance.store.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public String list(Model model,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "5") int size) {
        log.debug("GET /clients page={} size={}", page, size);
        Page<Client> result = clientService.findAll(PageRequest.of(page, size, Sort.by("name")));
        model.addAttribute("clients", result.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", result.getTotalPages());
        return "client/clients";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        log.debug("GET /clients/add");
        model.addAttribute("client", new ClientDto());
        return "client/newClient";
    }

    @PostMapping("/add-client")
    public String save(@Valid @ModelAttribute("client") ClientDto dto,
                       BindingResult result) {
        if (result.hasErrors()) {
            log.debug("Validation errors saving client: {}", result.getAllErrors());
            return "client/newClient";
        }
        Client client = new Client(null, dto.getName(), dto.getEmail(), passwordEncoder.encode(dto.getPassword()), dto.getCard());
        clientService.save(client);
        log.info("Client saved: name={}", client.getName());
        return "redirect:/clients";
    }

    @GetMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        log.info("Deleting client id={}", id);
        clientService.delete(id);
        return "redirect:/clients";
    }
}
