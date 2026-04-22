package com.vovan4ok.appliance.store.service.impl;

import com.vovan4ok.appliance.store.aspect.Loggable;
import com.vovan4ok.appliance.store.model.Manufacturer;
import com.vovan4ok.appliance.store.repository.ManufacturerRepository;
import com.vovan4ok.appliance.store.service.ManufacturerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ManufacturerServiceImpl implements ManufacturerService {

    private final ManufacturerRepository manufacturerRepository;

    @Loggable
    @Override
    public List<Manufacturer> findAll() {
        return manufacturerRepository.findAll();
    }

    @Override
    public Page<Manufacturer> findAll(Pageable pageable) {
        return manufacturerRepository.findAll(pageable);
    }

    @Override
    public Optional<Manufacturer> findById(Long id) {
        return manufacturerRepository.findById(id);
    }

    @Loggable
    @Transactional
    @Override
    public Manufacturer save(Manufacturer manufacturer) {
        return manufacturerRepository.save(manufacturer);
    }

    @Loggable
    @Transactional
    @Override
    public void delete(Long id) {
        manufacturerRepository.deleteById(id);
    }
}
