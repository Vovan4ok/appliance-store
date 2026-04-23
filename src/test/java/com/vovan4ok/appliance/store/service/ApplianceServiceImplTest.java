package com.vovan4ok.appliance.store.service;

import com.vovan4ok.appliance.store.model.Appliance;
import com.vovan4ok.appliance.store.model.Category;
import com.vovan4ok.appliance.store.model.Manufacturer;
import com.vovan4ok.appliance.store.model.PowerType;
import com.vovan4ok.appliance.store.repository.ApplianceRepository;
import com.vovan4ok.appliance.store.service.impl.ApplianceServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplianceServiceImplTest {

    @Mock
    ApplianceRepository applianceRepository;

    @InjectMocks
    ApplianceServiceImpl applianceService;

    private Appliance buildAppliance(Long id) {
        Manufacturer m = new Manufacturer(1L, "Samsung");
        return new Appliance(id, "Fridge", Category.BIG, "RB37", m,
                PowerType.AC220, "A++", "Big fridge", 100, BigDecimal.valueOf(500), 10);
    }

    @Test
    void findAll_returnsList() {
        List<Appliance> appliances = List.of(buildAppliance(1L), buildAppliance(2L));
        when(applianceRepository.findAll()).thenReturn(appliances);

        List<Appliance> result = applianceService.findAll();

        assertThat(result).hasSize(2);
        verify(applianceRepository).findAll();
    }

    @Test
    void findAll_pageable_returnsPage() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Appliance> page = new PageImpl<>(List.of(buildAppliance(1L)));
        when(applianceRepository.findAll(pageable)).thenReturn(page);

        Page<Appliance> result = applianceService.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(applianceRepository).findAll(pageable);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_withFilters_usesSpecification() {
        Pageable pageable = PageRequest.of(0, 6);
        Page<Appliance> page = new PageImpl<>(List.of(buildAppliance(1L)));
        when(applianceRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<Appliance> result = applianceService.findAll(
                "Fridge", Category.BIG, PowerType.AC220, 1L,
                BigDecimal.valueOf(100), BigDecimal.valueOf(1000), false, pageable
        );

        assertThat(result.getContent()).hasSize(1);
        verify(applianceRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_withNullFilters_usesSpecification() {
        Pageable pageable = PageRequest.of(0, 6);
        Page<Appliance> page = new PageImpl<>(List.of(buildAppliance(1L)));
        when(applianceRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<Appliance> result = applianceService.findAll(null, null, null, null, null, null, false, pageable);

        assertThat(result).isNotNull();
    }

    @Test
    void findById_found_returnsAppliance() {
        Appliance appliance = buildAppliance(1L);
        when(applianceRepository.findById(1L)).thenReturn(Optional.of(appliance));

        Optional<Appliance> result = applianceService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Fridge");
    }

    @Test
    void findById_notFound_returnsEmpty() {
        when(applianceRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Appliance> result = applianceService.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void save_delegatesToRepository() {
        Appliance appliance = buildAppliance(null);
        Appliance saved = buildAppliance(1L);
        when(applianceRepository.save(appliance)).thenReturn(saved);

        Appliance result = applianceService.save(appliance);

        assertThat(result.getId()).isEqualTo(1L);
        verify(applianceRepository).save(appliance);
    }

    @Test
    void delete_callsDeleteById() {
        applianceService.delete(1L);

        verify(applianceRepository).deleteById(1L);
    }
}
