package com.vovan4ok.appliance.store.repository;

import com.vovan4ok.appliance.store.model.Appliance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ApplianceRepository extends JpaRepository<Appliance, Long>, JpaSpecificationExecutor<Appliance> {
}