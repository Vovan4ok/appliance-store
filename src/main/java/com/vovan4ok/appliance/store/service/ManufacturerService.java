package com.vovan4ok.appliance.store.service;

import com.vovan4ok.appliance.store.model.Manufacturer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ManufacturerService {

    List<Manufacturer> findAll();

    Page<Manufacturer> findAll(Pageable pageable);

    Optional<Manufacturer> findById(Long id);

    Manufacturer save(Manufacturer manufacturer);

    void delete(Long id);
}
