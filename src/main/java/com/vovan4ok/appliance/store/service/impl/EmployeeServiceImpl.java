package com.vovan4ok.appliance.store.service.impl;

import com.vovan4ok.appliance.store.aspect.Loggable;
import com.vovan4ok.appliance.store.model.Employee;
import com.vovan4ok.appliance.store.repository.EmployeeRepository;
import com.vovan4ok.appliance.store.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;

    @Loggable
    @Override
    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    @Override
    public Page<Employee> findAll(Pageable pageable) {
        return employeeRepository.findAll(pageable);
    }

    @Override
    public Optional<Employee> findById(Long id) {
        return employeeRepository.findById(id);
    }

    @Loggable
    @Transactional
    @Override
    public Employee save(Employee employee) {
        return employeeRepository.save(employee);
    }

    @Loggable
    @Transactional
    @Override
    public void delete(Long id) {
        employeeRepository.deleteById(id);
    }

    @Override
    public Optional<Employee> findByEmail(String email) {
        return employeeRepository.findByEmail(email);
    }
}
