package com.vovan4ok.appliance.store.service;

import com.vovan4ok.appliance.store.model.Employee;
import com.vovan4ok.appliance.store.repository.EmployeeRepository;
import com.vovan4ok.appliance.store.service.impl.EmployeeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    EmployeeRepository employeeRepository;

    @InjectMocks
    EmployeeServiceImpl employeeService;

    @Test
    void findAll_returnsList() {
        List<Employee> employees = List.of(
                new Employee(1L, "Alice", "alice@mail.com", "hashed", "sales"),
                new Employee(2L, "Bob", "bob@mail.com", "hashed", "support")
        );
        when(employeeRepository.findAll()).thenReturn(employees);

        List<Employee> result = employeeService.findAll();

        assertThat(result).hasSize(2);
        verify(employeeRepository).findAll();
    }

    @Test
    void findAll_pageable_returnsPage() {
        Pageable pageable = PageRequest.of(0, 5);
        Employee employee = new Employee(1L, "Alice", "alice@mail.com", "hashed", "sales");
        Page<Employee> page = new PageImpl<>(List.of(employee));
        when(employeeRepository.findAll(pageable)).thenReturn(page);

        Page<Employee> result = employeeService.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(employeeRepository).findAll(pageable);
    }

    @Test
    void findById_found_returnsEmployee() {
        Employee employee = new Employee(1L, "Alice", "alice@mail.com", "hashed", "sales");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        Optional<Employee> result = employeeService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("alice@mail.com");
    }

    @Test
    void findById_notFound_returnsEmpty() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Employee> result = employeeService.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void save_delegatesToRepository() {
        Employee employee = new Employee(null, "Alice", "alice@mail.com", "hashed", "sales");
        Employee saved = new Employee(1L, "Alice", "alice@mail.com", "hashed", "sales");
        when(employeeRepository.save(employee)).thenReturn(saved);

        Employee result = employeeService.save(employee);

        assertThat(result.getId()).isEqualTo(1L);
        verify(employeeRepository).save(employee);
    }

    @Test
    void delete_callsDeleteById() {
        employeeService.delete(1L);

        verify(employeeRepository).deleteById(1L);
    }
}
