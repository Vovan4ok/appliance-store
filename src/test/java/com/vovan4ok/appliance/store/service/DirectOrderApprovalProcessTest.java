package com.vovan4ok.appliance.store.service;

import com.vovan4ok.appliance.store.service.impl.DirectOrderApprovalProcess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DirectOrderApprovalProcessTest {

    @Mock
    OrderService orderService;

    @InjectMocks
    DirectOrderApprovalProcess process;

    @Test
    void withoutTemporal_everyStepCallsTheOrderServiceDirectly() {
        process.submit(1L);
        process.approve(2L);
        process.unapprove(3L);

        verify(orderService).submitCart(1L);
        verify(orderService).approve(2L);
        verify(orderService).unapprove(3L);
    }
}
