package com.vovan4ok.appliance.store.repository;

import com.vovan4ok.appliance.store.model.Appliance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ApplianceRepository extends JpaRepository<Appliance, Long>, JpaSpecificationExecutor<Appliance> {

    // A missing stock value counts as out of stock, like everywhere else in the shop
    @Query("select count(a) from Appliance a where a.stock is null or a.stock <= 0")
    long countOutOfStock();
}
