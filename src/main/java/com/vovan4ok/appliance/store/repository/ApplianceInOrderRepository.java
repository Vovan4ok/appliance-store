package com.vovan4ok.appliance.store.repository;

import com.vovan4ok.appliance.store.model.OrderRow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplianceInOrderRepository extends JpaRepository<OrderRow, Long> {
}