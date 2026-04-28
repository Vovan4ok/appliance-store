package com.vovan4ok.appliance.store.controller;

import com.vovan4ok.appliance.store.model.Manufacturer;
import com.vovan4ok.appliance.store.model.dto.ManufacturerDto;
import com.vovan4ok.appliance.store.service.ManufacturerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/manufacturers")
@RequiredArgsConstructor
public class ManufacturerController {

    private final ManufacturerService manufacturerService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @GetMapping
    public String list(Model model,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "5") int size) {
        log.debug("GET /manufacturers page={} size={}", page, size);
        Page<Manufacturer> result = manufacturerService.findAll(PageRequest.of(page, size, Sort.by("name")));
        model.addAttribute("manufacturers", result.getContent().stream().map(ManufacturerDto::from).toList());
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
                       BindingResult result,
                       @RequestParam(required = false) MultipartFile logo) {
        if (result.hasErrors()) {
            log.debug("Validation errors saving manufacturer: {}", result.getAllErrors());
            return "manufacture/newManufacturer";
        }
        Manufacturer manufacturer = applyDto(new Manufacturer(), dto);
        if (logo != null && !logo.isEmpty()) {
            manufacturer.setLogoPath(saveLogo(logo));
        }
        manufacturerService.save(manufacturer);
        log.info("Manufacturer saved: name={}", manufacturer.getName());
        return "redirect:/manufacturers";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        log.debug("GET /manufacturers/{}/edit", id);
        Manufacturer m = manufacturerService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Manufacturer not found: " + id));
        model.addAttribute("manufacturer", ManufacturerDto.from(m));
        model.addAttribute("manufacturerId", id);
        model.addAttribute("currentLogoPath", m.getLogoPath());
        return "manufacture/editManufacturer";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("manufacturer") ManufacturerDto dto,
                         BindingResult result,
                         Model model,
                         @RequestParam(required = false) MultipartFile logo) {
        if (result.hasErrors()) {
            model.addAttribute("manufacturerId", id);
            return "manufacture/editManufacturer";
        }
        Manufacturer m = manufacturerService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Manufacturer not found: " + id));
        applyDto(m, dto);
        if (logo != null && !logo.isEmpty()) {
            m.setLogoPath(saveLogo(logo));
        }
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

    private Manufacturer applyDto(Manufacturer m, ManufacturerDto dto) {
        m.setName(dto.getName());
        m.setCountry(dto.getCountry());
        m.setWebsite(dto.getWebsite());
        m.setDescription(dto.getDescription());
        m.setFoundedYear(dto.getFoundedYear());
        return m;
    }

    private String saveLogo(MultipartFile file) {
        try {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return null;
            }
            String ext = contentType.equals("image/png") ? "png" : "jpg";
            String filename = UUID.randomUUID() + "." + ext;
            Path dest = Paths.get(uploadDir).resolve(filename);
            Files.createDirectories(dest.getParent());
            file.transferTo(dest.toFile());
            return filename;
        } catch (IOException e) {
            log.warn("Failed to save manufacturer logo: {}", e.getMessage());
            return null;
        }
    }
}