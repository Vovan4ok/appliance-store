package com.vovan4ok.appliance.store.controller;

import com.vovan4ok.appliance.store.model.Appliance;
import com.vovan4ok.appliance.store.model.Category;
import com.vovan4ok.appliance.store.model.PowerType;
import com.vovan4ok.appliance.store.model.dto.ApplianceDto;
import com.vovan4ok.appliance.store.model.dto.ManufacturerDto;
import com.vovan4ok.appliance.store.service.ApplianceService;
import com.vovan4ok.appliance.store.service.ManufacturerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Slf4j
@Controller
@RequestMapping("/appliances")
@RequiredArgsConstructor
public class ApplianceController {

    private final ApplianceService applianceService;
    private final ManufacturerService manufacturerService;

    @GetMapping
    public String list(Model model,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "6") int size,
                       @RequestParam(required = false) String name,
                       @RequestParam(required = false) Category category,
                       @RequestParam(required = false) PowerType powerType,
                       @RequestParam(required = false) Long manufacturerId,
                       @RequestParam(required = false) BigDecimal minPrice,
                       @RequestParam(required = false) BigDecimal maxPrice,
                       @RequestParam(defaultValue = "name") String sortBy,
                       @RequestParam(defaultValue = "asc") String sortDir,
                       @RequestParam(defaultValue = "false") boolean outOfStockOnly) {
        log.debug("GET /appliances page={} size={} name={} category={} powerType={}"
                + " manufacturerId={} sortBy={} sortDir={} outOfStockOnly={}",
                page, size, name, category, powerType, manufacturerId, sortBy, sortDir, outOfStockOnly);

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Page<Appliance> result = applianceService.findAll(
                name, category, powerType, manufacturerId, minPrice, maxPrice, false, outOfStockOnly,
                PageRequest.of(page, size, sort));

        model.addAttribute("appliances", result.getContent().stream().map(ApplianceDto::from).toList());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", result.getTotalPages());

        model.addAttribute("filterName", name);
        model.addAttribute("filterCategory", category);
        model.addAttribute("filterPowerType", powerType);
        model.addAttribute("filterManufacturerId", manufacturerId);
        model.addAttribute("filterMinPrice", minPrice);
        model.addAttribute("filterMaxPrice", maxPrice);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("filterOutOfStockOnly", outOfStockOnly);

        model.addAttribute("categories", Category.values());
        model.addAttribute("powerTypes", PowerType.values());
        model.addAttribute("manufacturers", manufacturerService.findAll().stream().map(ManufacturerDto::from).toList());

        return "appliance/appliances";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        log.debug("GET /appliances/add");
        model.addAttribute("appliance", new ApplianceDto());
        model.addAttribute("manufacturers", manufacturerService.findAll().stream().map(ManufacturerDto::from).toList());
        model.addAttribute("categories", Category.values());
        model.addAttribute("powerTypes", PowerType.values());
        return "appliance/newAppliance";
    }

    @PostMapping("/add")
    public String save(@Valid @ModelAttribute("appliance") ApplianceDto dto,
                       BindingResult result,
                       Model model) {
        if (result.hasErrors()) {
            log.debug("Validation errors saving appliance: {}", result.getAllErrors());
            model.addAttribute("manufacturers",
                    manufacturerService.findAll().stream().map(ManufacturerDto::from).toList());
            model.addAttribute("categories", Category.values());
            model.addAttribute("powerTypes", PowerType.values());
            return "appliance/newAppliance";
        }
        Appliance appliance = new Appliance();
        getApplianceFromDTO(dto, appliance);
        log.info("Appliance saved: name={}", appliance.getName());
        return "redirect:/appliances";
    }

    private void getApplianceFromDTO(@ModelAttribute("appliance") @Valid ApplianceDto dto, Appliance appliance) {
        appliance.setName(dto.getName());
        appliance.setCategory(dto.getCategory());
        appliance.setModel(dto.getModel());
        appliance.setManufacturer(manufacturerService.findById(dto.getManufacturerId()).orElse(null));
        appliance.setPowerType(dto.getPowerType());
        appliance.setCharacteristic(dto.getCharacteristic());
        appliance.setDescription(dto.getDescription());
        appliance.setPower(dto.getPower());
        appliance.setPrice(dto.getPrice());
        appliance.setStock(dto.getStock());
        applianceService.save(appliance);
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        log.debug("GET /appliances/{}/edit", id);
        Appliance appliance = applianceService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Appliance not found: " + id));
        ApplianceDto dto = ApplianceDto.from(appliance);
        model.addAttribute("appliance", dto);
        model.addAttribute("applianceId", id);
        model.addAttribute("manufacturers", manufacturerService.findAll().stream().map(ManufacturerDto::from).toList());
        model.addAttribute("categories", Category.values());
        model.addAttribute("powerTypes", PowerType.values());
        return "appliance/editAppliance";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("appliance") ApplianceDto dto,
                         BindingResult result,
                         Model model) {
        if (result.hasErrors()) {
            log.debug("Validation errors updating appliance id={}: {}", id, result.getAllErrors());
            model.addAttribute("applianceId", id);
            model.addAttribute("manufacturers",
                    manufacturerService.findAll().stream().map(ManufacturerDto::from).toList());
            model.addAttribute("categories", Category.values());
            model.addAttribute("powerTypes", PowerType.values());
            return "appliance/editAppliance";
        }
        Appliance appliance = applianceService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Appliance not found: " + id));
        getApplianceFromDTO(dto, appliance);
        log.info("Appliance updated id={} name={}", id, appliance.getName());
        return "redirect:/appliances";
    }

    @GetMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        log.info("Deleting appliance id={}", id);
        applianceService.delete(id);
        return "redirect:/appliances";
    }
}
