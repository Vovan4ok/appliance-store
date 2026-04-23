package com.vovan4ok.appliance.store.service;

import com.vovan4ok.appliance.store.model.Appliance;
import com.vovan4ok.appliance.store.model.Category;
import com.vovan4ok.appliance.store.model.PowerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ApplianceService {

    List<Appliance> findAll();

    Page<Appliance> findAll(Pageable pageable);

    Page<Appliance> findAll(
            String name,
            Category category,
            PowerType powerType,
            Long manufacturerId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            boolean inStockOnly,
            boolean outOfStockOnly,
            Pageable pageable
    );

    Optional<Appliance> findById(Long id);

    Appliance save(Appliance appliance);

    void delete(Long id);
}