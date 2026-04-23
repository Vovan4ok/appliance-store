package com.vovan4ok.appliance.store.service.impl;

import com.vovan4ok.appliance.store.aspect.Loggable;
import com.vovan4ok.appliance.store.model.Appliance;
import com.vovan4ok.appliance.store.model.Category;
import com.vovan4ok.appliance.store.model.PowerType;
import com.vovan4ok.appliance.store.repository.ApplianceRepository;
import com.vovan4ok.appliance.store.repository.spec.ApplianceSpecification;
import com.vovan4ok.appliance.store.service.ApplianceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApplianceServiceImpl implements ApplianceService {

    private final ApplianceRepository applianceRepository;

    @Loggable
    @Override
    public List<Appliance> findAll() {
        return applianceRepository.findAll();
    }

    @Override
    public Page<Appliance> findAll(Pageable pageable) {
        return applianceRepository.findAll(pageable);
    }

    @Override
    public Page<Appliance> findAll(
            String name,
            Category category,
            PowerType powerType,
            Long manufacturerId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            boolean inStockOnly,
            boolean outOfStockOnly,
            Pageable pageable
    ) {
        return applianceRepository.findAll(
                ApplianceSpecification.withFilters(name, category, powerType, manufacturerId, minPrice, maxPrice, inStockOnly, outOfStockOnly),
                pageable
        );
    }

    @Override
    public Optional<Appliance> findById(Long id) {
        return applianceRepository.findById(id);
    }

    @Loggable
    @Transactional
    @Override
    public Appliance save(Appliance appliance) {
        return applianceRepository.save(appliance);
    }

    @Loggable
    @Transactional
    @Override
    public void delete(Long id) {
        applianceRepository.deleteById(id);
    }
}