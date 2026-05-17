package com.vovan4ok.appliance.store.service;

import com.vovan4ok.appliance.store.event.OrderApprovedEvent;
import com.vovan4ok.appliance.store.event.OrderSubmittedEvent;
import com.vovan4ok.appliance.store.model.Appliance;
import com.vovan4ok.appliance.store.model.OrderRow;
import com.vovan4ok.appliance.store.model.Orders;
import com.vovan4ok.appliance.store.repository.ApplianceInOrderRepository;
import com.vovan4ok.appliance.store.repository.ApplianceRepository;
import com.vovan4ok.appliance.store.repository.OrdersRepository;
import com.vovan4ok.appliance.store.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    OrdersRepository ordersRepository;
    @Mock
    ApplianceRepository applianceRepository;
    @Mock
    ApplianceInOrderRepository applianceInOrderRepository;
    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    OrderServiceImpl orderService;

    @Test
    void findAll_returnsList() {
        Orders order = new Orders();
        when(ordersRepository.findAll()).thenReturn(List.of(order));

        List<Orders> result = orderService.findAll();

        assertThat(result).hasSize(1);
    }

    @Test
    void findAll_pageable_returnsPage() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Orders> page = new PageImpl<>(List.of(new Orders()));
        when(ordersRepository.findAll(pageable)).thenReturn(page);

        Page<Orders> result = orderService.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void findById_found_returnsOrder() {
        Orders order = new Orders();
        order.setId(1L);
        when(ordersRepository.findById(1L)).thenReturn(Optional.of(order));

        Optional<Orders> result = orderService.findById(1L);

        assertThat(result).isPresent();
    }

    @Test
    void save_withNullApproved_setsToFalse() {
        Orders order = new Orders();
        order.setApproved(null);
        when(ordersRepository.save(order)).thenReturn(order);

        orderService.save(order);

        assertThat(order.getApproved()).isFalse();
        verify(ordersRepository).save(order);
    }

    @Test
    void save_withApprovedTrue_keepsValue() {
        Orders order = new Orders();
        order.setApproved(true);
        when(ordersRepository.save(order)).thenReturn(order);

        orderService.save(order);

        assertThat(order.getApproved()).isTrue();
    }

    @Test
    void delete_callsDeleteById() {
        orderService.delete(1L);

        verify(ordersRepository).deleteById(1L);
    }

    @Test
    void approve_setsApprovedTrue() {
        Orders order = new Orders();
        order.setId(1L);
        order.setApproved(false);
        when(ordersRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.approve(1L);

        assertThat(order.getApproved()).isTrue();
        verify(ordersRepository).save(order);
        verify(eventPublisher).publishEvent(any(OrderApprovedEvent.class));
    }

    @Test
    void approve_orderNotFound_throwsIllegalArgument() {
        when(ordersRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.approve(99L))
                .isInstanceOf(IllegalArgumentException.class);

        verify(ordersRepository, never()).save(any());
    }

    @Test
    void unapprove_setsApprovedNull() {
        Orders order = new Orders();
        order.setId(1L);
        order.setApproved(true);
        when(ordersRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.unapprove(1L);

        assertThat(order.getApproved()).isNull();
        verify(ordersRepository).save(order);
    }

    @Test
    void unapprove_orderNotFound_doesNothing() {
        when(ordersRepository.findById(99L)).thenReturn(Optional.empty());

        orderService.unapprove(99L);

        verify(ordersRepository, never()).save(any());
    }

    @Test
    void addOrderRow_createsAndSavesRow() {
        Orders order = new Orders();
        order.setId(1L);
        order.setOrderRowSet(new HashSet<>());
        Appliance appliance = new Appliance();
        appliance.setId(2L);

        when(ordersRepository.findById(1L)).thenReturn(Optional.of(order));
        when(applianceRepository.findById(2L)).thenReturn(Optional.of(appliance));

        orderService.addOrderRow(1L, 2L, 3L, BigDecimal.valueOf(100));

        assertThat(order.getOrderRowSet()).hasSize(1);
        verify(ordersRepository).save(order);
    }

    @Test
    void addOrderRow_orderNotFound_throwsException() {
        when(ordersRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.addOrderRow(99L, 1L, 1L, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void addOrderRow_applianceNotFound_throwsException() {
        Orders order = new Orders();
        order.setId(1L);
        when(ordersRepository.findById(1L)).thenReturn(Optional.of(order));
        when(applianceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.addOrderRow(1L, 99L, 1L, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Appliance not found");
    }

    @Test
    void removeOrderRow_multipleRows_savesOrderWithoutRow() {
        OrderRow target = new OrderRow();
        target.setId(5L);
        OrderRow other = new OrderRow();
        other.setId(6L);
        Orders order = new Orders();
        order.setId(1L);
        Set<OrderRow> rows = new HashSet<>();
        rows.add(target);
        rows.add(other);
        order.setOrderRowSet(rows);
        when(ordersRepository.findByOrderRowSet_Id(5L)).thenReturn(Optional.of(order));

        orderService.removeOrderRow(5L);

        assertThat(order.getOrderRowSet()).hasSize(1);
        verify(ordersRepository).save(order);
        verify(ordersRepository, never()).deleteById(any());
    }

    @Test
    void removeOrderRow_lastRow_deletesOrder() {
        OrderRow target = new OrderRow();
        target.setId(5L);
        Orders order = new Orders();
        order.setId(1L);
        Set<OrderRow> rows = new HashSet<>();
        rows.add(target);
        order.setOrderRowSet(rows);
        when(ordersRepository.findByOrderRowSet_Id(5L)).thenReturn(Optional.of(order));

        orderService.removeOrderRow(5L);

        verify(ordersRepository).deleteById(1L);
        verify(ordersRepository, never()).save(any());
    }

    @Test
    void updateOrderRowQuantity_updatesNumber() {
        OrderRow row = new OrderRow();
        row.setId(1L);
        row.setNumber(1L);
        when(applianceInOrderRepository.findById(1L)).thenReturn(Optional.of(row));

        orderService.updateOrderRowQuantity(1L, 5L);

        assertThat(row.getNumber()).isEqualTo(5L);
        verify(applianceInOrderRepository).save(row);
    }

    @Test
    void updateOrderRowQuantity_rowNotFound_doesNothing() {
        when(applianceInOrderRepository.findById(99L)).thenReturn(Optional.empty());

        orderService.updateOrderRowQuantity(99L, 5L);

        verify(applianceInOrderRepository, never()).save(any());
    }

    @Test
    void getOrderRows_orderFound_returnsRows() {
        OrderRow row = new OrderRow();
        Orders order = new Orders();
        order.setId(1L);
        order.setOrderRowSet(Set.of(row));
        when(ordersRepository.findById(1L)).thenReturn(Optional.of(order));

        List<OrderRow> result = orderService.getOrderRows(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getOrderRows_orderNotFound_returnsEmptyList() {
        when(ordersRepository.findById(99L)).thenReturn(Optional.empty());

        List<OrderRow> result = orderService.getOrderRows(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void findByClientEmail_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Orders> page = new PageImpl<>(List.of(new Orders()));
        when(ordersRepository.findByClient_Email("user@mail.com", pageable)).thenReturn(page);

        Page<Orders> result = orderService.findByClientEmail("user@mail.com", pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void findPendingByClientEmail_delegatesToRepository() {
        Orders order = new Orders();
        when(ordersRepository.findFirstByClient_EmailAndApprovedFalseOrderByIdDesc("user@mail.com"))
                .thenReturn(Optional.of(order));

        Optional<Orders> result = orderService.findPendingByClientEmail("user@mail.com");

        assertThat(result).isPresent();
    }

    @Test
    void submitCart_setsApprovedNull() {
        Orders order = new Orders();
        order.setId(1L);
        order.setApproved(false);
        when(ordersRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.submitCart(1L);

        assertThat(order.getApproved()).isNull();
        verify(ordersRepository).save(order);
        verify(eventPublisher).publishEvent(any(OrderSubmittedEvent.class));
    }

    @Test
    void submitCart_orderNotFound_doesNothing() {
        when(ordersRepository.findById(99L)).thenReturn(Optional.empty());

        orderService.submitCart(99L);

        verify(ordersRepository, never()).save(any());
    }
}
