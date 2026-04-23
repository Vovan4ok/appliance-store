package com.vovan4ok.appliance.store.service;

import com.vovan4ok.appliance.store.model.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface EmployeeService {

    List<Employee> findAll();

    Page<Employee> findAll(Pageable pageable);

    Optional<Employee> findById(Long id);

    Employee save(Employee employee);

    void delete(Long id);

    java.util.Optional<Employee> findByEmail(String email);

    void updateAvatar(Long id, String avatarPath);
}
