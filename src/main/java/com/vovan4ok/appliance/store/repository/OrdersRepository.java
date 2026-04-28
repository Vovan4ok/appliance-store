package com.vovan4ok.appliance.store.repository;

import com.vovan4ok.appliance.store.model.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrdersRepository extends JpaRepository<Orders, Long> {
    Optional<Orders> findFirstByClient_EmailAndApprovedFalseOrderByIdDesc(String email);
    Page<Orders> findByClient_Email(String email, Pageable pageable);
    Optional<Orders> findByOrderRowSet_Id(Long rowId);
}