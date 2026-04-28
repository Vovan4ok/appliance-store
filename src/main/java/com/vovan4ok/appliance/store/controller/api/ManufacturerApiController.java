package com.vovan4ok.appliance.store.controller.api;

import com.vovan4ok.appliance.store.model.Manufacturer;
import com.vovan4ok.appliance.store.model.dto.api.ManufacturerRequest;
import com.vovan4ok.appliance.store.model.dto.api.ManufacturerResponse;
import com.vovan4ok.appliance.store.model.dto.api.PageResponse;
import com.vovan4ok.appliance.store.service.ManufacturerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/manufacturers")
@RequiredArgsConstructor
@Tag(name = "Manufacturers", description = "CRUD operations for manufacturers")
public class ManufacturerApiController {

    private final ManufacturerService manufacturerService;

    @GetMapping
    @Operation(summary = "List manufacturers (paginated)")
    public PageResponse<ManufacturerResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        return PageResponse.from(
                manufacturerService.findAll(PageRequest.of(page, size, sort))
                        .map(ManufacturerResponse::from));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get manufacturer by ID")
    public ManufacturerResponse getById(@PathVariable Long id) {
        return manufacturerService.findById(id)
                .map(ManufacturerResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Manufacturer not found: " + id));
    }

    @PostMapping
    @Operation(summary = "Create a new manufacturer (EMPLOYEE only)")
    public ResponseEntity<ManufacturerResponse> create(@Valid @RequestBody ManufacturerRequest request) {
        Manufacturer manufacturer = new Manufacturer();
        manufacturer.setName(request.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ManufacturerResponse.from(manufacturerService.save(manufacturer)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a manufacturer (EMPLOYEE only)")
    public ManufacturerResponse update(@PathVariable Long id, @Valid @RequestBody ManufacturerRequest request) {
        Manufacturer manufacturer = manufacturerService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Manufacturer not found: " + id));
        manufacturer.setName(request.getName());
        return ManufacturerResponse.from(manufacturerService.save(manufacturer));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a manufacturer (EMPLOYEE only)")
    public void delete(@PathVariable Long id) {
        manufacturerService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Manufacturer not found: " + id));
        manufacturerService.delete(id);
    }
}