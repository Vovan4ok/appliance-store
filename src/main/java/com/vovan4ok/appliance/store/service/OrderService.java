package com.vovan4ok.appliance.store.service;

import com.vovan4ok.appliance.store.model.OrderRow;
import com.vovan4ok.appliance.store.model.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrderService {

    List<Orders> findAll();

    Page<Orders> findAll(Pageable pageable);

    Page<Orders> findByClientEmail(String email, Pageable pageable);

    Optional<Orders> findById(Long id);

    Orders save(Orders orders);

    void delete(Long id);

    void approve(Long id);

    void unapprove(Long id);

    void submitCart(Long orderId);

    void addOrderRow(Long orderId, Long applianceId, Long number, BigDecimal price);

    void removeOrderRow(Long rowId);

    void updateOrderRowQuantity(Long rowId, Long number);

    List<OrderRow> getOrderRows(Long orderId);

    Optional<Orders> findPendingByClientEmail(String email);
}
