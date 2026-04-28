package com.vovan4ok.appliance.store.service;

import com.vovan4ok.appliance.store.model.Manufacturer;
import com.vovan4ok.appliance.store.repository.ManufacturerRepository;
import com.vovan4ok.appliance.store.service.impl.ManufacturerServiceImpl;
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
class ManufacturerServiceImplTest {

    @Mock
    ManufacturerRepository manufacturerRepository;

    @InjectMocks
    ManufacturerServiceImpl manufacturerService;

    private static Manufacturer mfr(Long id, String name) {
        Manufacturer m = new Manufacturer();
        m.setId(id);
        m.setName(name);
        return m;
    }

    @Test
    void findAll_returnsList() {
        List<Manufacturer> manufacturers = List.of(mfr(1L, "Samsung"), mfr(2L, "LG"));
        when(manufacturerRepository.findAll()).thenReturn(manufacturers);

        List<Manufacturer> result = manufacturerService.findAll();

        assertThat(result).hasSize(2);
        verify(manufacturerRepository).findAll();
    }

    @Test
    void findAll_pageable_returnsPage() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Manufacturer> page = new PageImpl<>(List.of(mfr(1L, "Samsung")));
        when(manufacturerRepository.findAll(pageable)).thenReturn(page);

        Page<Manufacturer> result = manufacturerService.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(manufacturerRepository).findAll(pageable);
    }

    @Test
    void findById_found_returnsOptional() {
        Manufacturer manufacturer = mfr(1L, "Samsung");
        when(manufacturerRepository.findById(1L)).thenReturn(Optional.of(manufacturer));

        Optional<Manufacturer> result = manufacturerService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Samsung");
    }

    @Test
    void findById_notFound_returnsEmpty() {
        when(manufacturerRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Manufacturer> result = manufacturerService.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void save_delegatesToRepository() {
        Manufacturer manufacturer = mfr(null, "Bosch");
        Manufacturer saved = mfr(1L, "Bosch");
        when(manufacturerRepository.save(manufacturer)).thenReturn(saved);

        Manufacturer result = manufacturerService.save(manufacturer);

        assertThat(result.getId()).isEqualTo(1L);
        verify(manufacturerRepository).save(manufacturer);
    }

    @Test
    void delete_callsDeleteById() {
        manufacturerService.delete(1L);

        verify(manufacturerRepository).deleteById(1L);
    }
}