package com.vovan4ok.appliance.store.repository;

import com.vovan4ok.appliance.store.model.Manufacturer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManufacturerRepository extends JpaRepository<Manufacturer, Long> {
    Page<Manufacturer> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
