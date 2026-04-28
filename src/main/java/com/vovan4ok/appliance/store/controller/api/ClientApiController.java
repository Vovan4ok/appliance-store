package com.vovan4ok.appliance.store.controller.api;

import com.vovan4ok.appliance.store.model.dto.api.ClientResponse;
import com.vovan4ok.appliance.store.model.dto.api.PageResponse;
import com.vovan4ok.appliance.store.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Tag(name = "Clients", description = "Client management (EMPLOYEE only)")
public class ClientApiController {

    private final ClientService clientService;

    @GetMapping
    @Operation(summary = "List clients (paginated)")
    public PageResponse<ClientResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        return PageResponse.from(
                clientService.findAll(PageRequest.of(page, size, sort))
                        .map(ClientResponse::from));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get client by ID")
    public ClientResponse getById(@PathVariable Long id) {
        return clientService.findById(id)
                .map(ClientResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + id));
    }
}