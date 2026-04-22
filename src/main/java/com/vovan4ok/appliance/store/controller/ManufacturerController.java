package com.vovan4ok.appliance.store.controller;

import com.vovan4ok.appliance.store.model.Manufacturer;
import com.vovan4ok.appliance.store.model.dto.ManufacturerDto;
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
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("/manufacturers")
@RequiredArgsConstructor
public class ManufacturerController {

    private final ManufacturerService manufacturerService;

    @GetMapping
    public String list(Model model,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "5") int size) {
        log.debug("GET /manufacturers page={} size={}", page, size);
        Page<Manufacturer> result = manufacturerService.findAll(PageRequest.of(page, size, Sort.by("name")));
        model.addAttribute("manufacturers", result.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", result.getTotalPages());
        return "manufacture/manufacturers";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        log.debug("GET /manufacturers/add");
        model.addAttribute("manufacturer", new ManufacturerDto());
        return "manufacture/newManufacturer";
    }

    @PostMapping("/add-manufacturer")
    public String save(@Valid @ModelAttribute("manufacturer") ManufacturerDto dto,
                       BindingResult result) {
        if (result.hasErrors()) {
            log.debug("Validation errors saving manufacturer: {}", result.getAllErrors());
            return "manufacture/newManufacturer";
        }
        Manufacturer manufacturer = new Manufacturer(null, dto.getName());
        manufacturerService.save(manufacturer);
        log.info("Manufacturer saved: name={}", manufacturer.getName());
        return "redirect:/manufacturers";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        log.debug("GET /manufacturers/{}/edit", id);
        Manufacturer m = manufacturerService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Manufacturer not found: " + id));
        ManufacturerDto dto = new ManufacturerDto();
        dto.setName(m.getName());
        model.addAttribute("manufacturer", dto);
        model.addAttribute("manufacturerId", id);
        return "manufacture/editManufacturer";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("manufacturer") ManufacturerDto dto,
                         BindingResult result,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("manufacturerId", id);
            return "manufacture/editManufacturer";
        }
        Manufacturer m = manufacturerService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Manufacturer not found: " + id));
        m.setName(dto.getName());
        manufacturerService.save(m);
        log.info("Manufacturer updated id={} name={}", id, dto.getName());
        return "redirect:/manufacturers";
    }

    @GetMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        log.info("Deleting manufacturer id={}", id);
        manufacturerService.delete(id);
        return "redirect:/manufacturers";
    }
}
