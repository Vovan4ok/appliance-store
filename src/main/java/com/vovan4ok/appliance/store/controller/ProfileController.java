package com.vovan4ok.appliance.store.controller;

import com.vovan4ok.appliance.store.event.PasswordChangedEvent;
import com.vovan4ok.appliance.store.model.Client;
import com.vovan4ok.appliance.store.model.Employee;
import com.vovan4ok.appliance.store.model.User;
import com.vovan4ok.appliance.store.model.dto.PasswordChangeDto;
import com.vovan4ok.appliance.store.model.dto.ProfileDto;
import com.vovan4ok.appliance.store.service.ClientService;
import com.vovan4ok.appliance.store.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ClientService clientService;
    private final EmployeeService employeeService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @GetMapping
    public String profile(Authentication auth, Model model) {
        User user = loadUser(auth.getName());
        model.addAttribute("profile", buildProfileDto(user));
        model.addAttribute("passwordForm", new PasswordChangeDto());
        model.addAttribute("user", user);
        model.addAttribute("isEmployee", user instanceof Employee);
        return "profile";
    }

    @PostMapping("/edit")
    public String editProfile(@Valid @ModelAttribute("profile") ProfileDto dto,
                              BindingResult result,
                              Authentication auth,
                              Model model) {
        if (result.hasErrors()) {
            User user = loadUser(auth.getName());
            model.addAttribute("passwordForm", new PasswordChangeDto());
            model.addAttribute("user", user);
            model.addAttribute("isEmployee", user instanceof Employee);
            model.addAttribute("currentUri", "/profile");
            return "profile";
        }
        User user = loadUser(auth.getName());
        user.setName(dto.getName());
        user.setPhone(dto.getPhone());
        user.setDateOfBirth(dto.getDateOfBirth());
        if (user instanceof Client c) {
            c.setCard(dto.getCard());
        }
        if (user instanceof Employee e) {
            e.setDepartment(dto.getDepartment());
        }
        saveUser(user);
        log.info("Profile updated for {}", auth.getName());
        return "redirect:/profile?updated";
    }

    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") PasswordChangeDto dto,
                                 BindingResult result,
                                 Authentication auth,
                                 Model model) {
        User user = loadUser(auth.getName());

        if (result.hasErrors()) {
            model.addAttribute("profile", buildProfileDto(user));
            model.addAttribute("user", user);
            model.addAttribute("isEmployee", user instanceof Employee);
            model.addAttribute("currentUri", "/profile");
            return "profile";
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            model.addAttribute("profile", buildProfileDto(user));
            model.addAttribute("user", user);
            model.addAttribute("isEmployee", user instanceof Employee);
            model.addAttribute("wrongPassword", true);
            model.addAttribute("currentUri", "/profile");
            return "profile";
        }
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            model.addAttribute("profile", buildProfileDto(user));
            model.addAttribute("user", user);
            model.addAttribute("isEmployee", user instanceof Employee);
            model.addAttribute("passwordMismatch", true);
            model.addAttribute("currentUri", "/profile");
            return "profile";
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        saveUser(user);
        log.info("Password changed for {}", auth.getName());
        eventPublisher.publishEvent(new PasswordChangedEvent(user.getName(), user.getEmail()));
        return "redirect:/profile?passwordChanged";
    }

    @PostMapping("/avatar")
    public String uploadAvatar(@RequestParam("avatar") MultipartFile file,
                               Authentication auth) {
        if (file.isEmpty()) {
            return "redirect:/profile?avatarError";
        }

        String contentType = file.getContentType();
        if (!List.of("image/jpeg", "image/png").contains(contentType)) {
            return "redirect:/profile?avatarError";
        }
        if (file.getSize() > 2L * 1024 * 1024) {
            return "redirect:/profile?avatarError";
        }

        String ext = "image/png".equals(contentType) ? ".png" : ".jpg";
        String filename = UUID.randomUUID() + ext;

        try {
            Path dir = Paths.get(uploadDir);
            Files.createDirectories(dir);

            User user = loadUser(auth.getName());
            if (user.getAvatarPath() != null) {
                Files.deleteIfExists(dir.resolve(user.getAvatarPath()));
            }
            file.transferTo(dir.resolve(filename));

            if (user instanceof Client c) {
                clientService.updateAvatar(c.getId(), filename);
            } else if (user instanceof Employee e) {
                employeeService.updateAvatar(e.getId(), filename);
            }

        } catch (IOException ex) {
            log.error("Avatar upload failed for {}: {}", auth.getName(), ex.getMessage());
            return "redirect:/profile?avatarError";
        }

        log.info("Avatar updated for {}", auth.getName());
        return "redirect:/profile?avatarUpdated";
    }

    private User loadUser(String email) {
        return employeeService.findByEmail(email)
                .<User>map(e -> e)
                .orElseGet(() -> clientService.findByEmail(email)
                        .orElseThrow(() -> new IllegalArgumentException("User not found: " + email)));
    }

    private void saveUser(User user) {
        if (user instanceof Employee e) {
            employeeService.save(e);
        } else if (user instanceof Client c) {
            clientService.save(c);
        }
    }

    private ProfileDto buildProfileDto(User user) {
        ProfileDto dto = new ProfileDto();
        dto.setName(user.getName());
        dto.setPhone(user.getPhone());
        dto.setDateOfBirth(user.getDateOfBirth());
        if (user instanceof Client c) {
            dto.setCard(c.getCard());
        }
        if (user instanceof Employee e) {
            dto.setDepartment(e.getDepartment());
        }
        return dto;
    }
}