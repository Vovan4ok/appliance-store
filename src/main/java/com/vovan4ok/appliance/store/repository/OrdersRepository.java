package com.vovan4ok.appliance.store.repository;

import com.vovan4ok.appliance.store.model.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdersRepository extends JpaRepository<Orders, Long> {
    java.util.Optional<Orders> findFirstByClient_EmailAndApprovedFalseOrderByIdDesc(String email);
    org.springframework.data.domain.Page<Orders> findByClient_Email(String email, org.springframework.data.domain.Pageable pageable);
    java.util.Optional<Orders> findByOrderRowSet_Id(Long rowId);
}
