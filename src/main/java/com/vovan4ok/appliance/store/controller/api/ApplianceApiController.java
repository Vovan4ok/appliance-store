package com.vovan4ok.appliance.store.controller.api;

import com.vovan4ok.appliance.store.model.Appliance;
import com.vovan4ok.appliance.store.model.Category;
import com.vovan4ok.appliance.store.model.PowerType;
import com.vovan4ok.appliance.store.model.dto.api.ApplianceRequest;
import com.vovan4ok.appliance.store.model.dto.api.ApplianceResponse;
import com.vovan4ok.appliance.store.model.dto.api.PageResponse;
import com.vovan4ok.appliance.store.service.ApplianceService;
import com.vovan4ok.appliance.store.service.ManufacturerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/appliances")
@RequiredArgsConstructor
@Tag(name = "Appliances", description = "CRUD operations for appliances")
public class ApplianceApiController {

    private final ApplianceService applianceService;
    private final ManufacturerService manufacturerService;

    @GetMapping
    @Operation(summary = "List appliances (paginated, filterable)")
    public PageResponse<ApplianceResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) PowerType powerType,
            @RequestParam(required = false) Long manufacturerId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "false") boolean inStockOnly) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Page<ApplianceResponse> result = applianceService
                .findAll(name, category, powerType, manufacturerId, minPrice, maxPrice,
                        inStockOnly, false, PageRequest.of(page, size, sort))
                .map(ApplianceResponse::from);

        return PageResponse.from(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appliance by ID")
    public ApplianceResponse getById(@PathVariable Long id) {
        return applianceService.findById(id)
                .map(ApplianceResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Appliance not found: " + id));
    }

    @PostMapping
    @Operation(summary = "Create a new appliance (EMPLOYEE only)")
    public ResponseEntity<ApplianceResponse> create(@Valid @RequestBody ApplianceRequest request) {
        Appliance appliance = buildAppliance(new Appliance(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApplianceResponse.from(appliance));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an appliance (EMPLOYEE only)")
    public ApplianceResponse update(@PathVariable Long id, @Valid @RequestBody ApplianceRequest request) {
        Appliance appliance = applianceService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Appliance not found: " + id));
        return ApplianceResponse.from(buildAppliance(appliance, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an appliance (EMPLOYEE only)")
    public void delete(@PathVariable Long id) {
        applianceService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Appliance not found: " + id));
        applianceService.delete(id);
    }

    private Appliance buildAppliance(Appliance appliance, ApplianceRequest request) {
        appliance.setName(request.getName());
        appliance.setModel(request.getModel());
        appliance.setCategory(request.getCategory());
        appliance.setPowerType(request.getPowerType());
        appliance.setManufacturer(manufacturerService.findById(request.getManufacturerId())
                .orElseThrow(() -> new IllegalArgumentException("Manufacturer not found: " + request.getManufacturerId())));
        appliance.setCharacteristic(request.getCharacteristic());
        appliance.setDescription(request.getDescription());
        appliance.setPower(request.getPower());
        appliance.setPrice(request.getPrice());
        appliance.setStock(request.getStock());
        return applianceService.save(appliance);
    }
}