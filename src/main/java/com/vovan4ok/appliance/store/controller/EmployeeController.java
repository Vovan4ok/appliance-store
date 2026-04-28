package com.vovan4ok.appliance.store.controller;

import com.vovan4ok.appliance.store.model.Employee;
import com.vovan4ok.appliance.store.model.dto.EmployeeDto;
import com.vovan4ok.appliance.store.model.dto.EmployeeViewDto;
import com.vovan4ok.appliance.store.service.EmployeeService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public String list(Model model,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "5") int size) {
        log.debug("GET /employees page={} size={}", page, size);
        Page<Employee> result = employeeService.findAll(PageRequest.of(page, size, Sort.by("name")));
        model.addAttribute("employees", result.getContent().stream().map(EmployeeViewDto::from).toList());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", result.getTotalPages());
        return "employee/employees";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        log.debug("GET /employees/add");
        model.addAttribute("employee", new EmployeeDto());
        return "employee/newEmployee";
    }

    @PostMapping("/add-employee")
    public String save(@Valid @ModelAttribute("employee") EmployeeDto dto,
                       BindingResult result) {
        if (result.hasErrors()) {
            log.debug("Validation errors saving employee: {}", result.getAllErrors());
            return "employee/newEmployee";
        }
        Employee employee = new Employee(null, dto.getName(), dto.getEmail(),
                passwordEncoder.encode(dto.getPassword()), dto.getDepartment());
        employee.setPhone(dto.getPhone());
        employee.setDateOfBirth(dto.getDateOfBirth());
        employeeService.save(employee);
        log.info("Employee saved: name={}", employee.getName());
        return "redirect:/employees";
    }

    @GetMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        log.info("Deleting employee id={}", id);
        employeeService.delete(id);
        return "redirect:/employees";
    }
}
