package com.vovan4ok.appliance.store.service.impl;

import com.vovan4ok.appliance.store.aspect.Loggable;
import com.vovan4ok.appliance.store.exception.InsufficientStockException;
import com.vovan4ok.appliance.store.model.Appliance;
import com.vovan4ok.appliance.store.model.OrderRow;
import com.vovan4ok.appliance.store.model.Orders;
import com.vovan4ok.appliance.store.repository.ApplianceInOrderRepository;
import com.vovan4ok.appliance.store.repository.ApplianceRepository;
import com.vovan4ok.appliance.store.repository.OrdersRepository;
import com.vovan4ok.appliance.store.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrdersRepository ordersRepository;
    private final ApplianceRepository applianceRepository;
    private final ApplianceInOrderRepository applianceInOrderRepository;

    @Loggable
    @Override
    public List<Orders> findAll() {
        return ordersRepository.findAll();
    }

    @Override
    public Page<Orders> findAll(Pageable pageable) {
        return ordersRepository.findAll(pageable);
    }

    @Override
    public Page<Orders> findByClientEmail(String email, Pageable pageable) {
        return ordersRepository.findByClient_Email(email, pageable);
    }

    @Override
    public Optional<Orders> findById(Long id) {
        return ordersRepository.findById(id);
    }

    @Loggable
    @Transactional
    @Override
    public Orders save(Orders orders) {
        if (orders.getApproved() == null) {
            orders.setApproved(false);
        }
        return ordersRepository.save(orders);
    }

    @Loggable
    @Transactional
    @Override
    public void delete(Long id) {
        ordersRepository.deleteById(id);
    }

    @Loggable
    @Transactional
    @Override
    public void approve(Long id) {
        Orders order = ordersRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

        for (OrderRow row : order.getOrderRowSet()) {
            Appliance appliance = row.getAppliance();
            int available = appliance.getStock() != null ? appliance.getStock() : 0;
            if (available < row.getNumber()) {
                throw new InsufficientStockException(appliance.getName(), available);
            }
        }

        for (OrderRow row : order.getOrderRowSet()) {
            Appliance appliance = row.getAppliance();
            appliance.setStock(appliance.getStock() - row.getNumber().intValue());
            applianceRepository.save(appliance);
        }

        order.setApproved(true);
        ordersRepository.save(order);
    }

    @Loggable
    @Transactional
    @Override
    public void unapprove(Long id) {
        ordersRepository.findById(id).ifPresent(order -> {
            order.setApproved(null);
            ordersRepository.save(order);
        });
    }

    @Loggable
    @Transactional
    @Override
    public void removeOrderRow(Long rowId) {
        ordersRepository.findByOrderRowSet_Id(rowId).ifPresent(order -> {
            order.getOrderRowSet().removeIf(row -> rowId.equals(row.getId()));
            if (order.getOrderRowSet().isEmpty()) {
                ordersRepository.deleteById(order.getId());
            } else {
                ordersRepository.save(order);
            }
        });
    }

    @Loggable
    @Transactional
    @Override
    public void updateOrderRowQuantity(Long rowId, Long number) {
        applianceInOrderRepository.findById(rowId).ifPresent(row -> {
            row.setNumber(number);
            applianceInOrderRepository.save(row);
        });
    }

    @Loggable
    @Transactional
    @Override
    public void addOrderRow(Long orderId, Long applianceId, Long number, BigDecimal price) {
        Orders orders = ordersRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        Appliance appliance = applianceRepository.findById(applianceId)
                .orElseThrow(() -> new IllegalArgumentException("Appliance not found: " + applianceId));

        OrderRow row = new OrderRow();
        row.setAppliance(appliance);
        row.setNumber(number);
        row.setAmount(price);

        orders.getOrderRowSet().add(row);
        ordersRepository.save(orders);
    }

    @Override
    public List<OrderRow> getOrderRows(Long orderId) {
        return ordersRepository.findById(orderId)
                .map(o -> new ArrayList<>(o.getOrderRowSet()))
                .orElse(new ArrayList<>());
    }

    @Override
    public Optional<OrderRow> findOrderRowById(Long rowId) {
        return applianceInOrderRepository.findById(rowId);
    }

    @Loggable
    @Transactional
    @Override
    public void submitCart(Long orderId) {
        ordersRepository.findById(orderId).ifPresent(order -> {
            order.setApproved(null);
            ordersRepository.save(order);
        });
    }

    @Override
    public Optional<Orders> findPendingByClientEmail(String email) {
        return ordersRepository.findFirstByClient_EmailAndApprovedFalseOrderByIdDesc(email);
    }
}
