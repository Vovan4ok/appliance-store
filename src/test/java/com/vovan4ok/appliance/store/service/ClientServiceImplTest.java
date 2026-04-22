package com.vovan4ok.appliance.store.service;

import com.vovan4ok.appliance.store.model.Client;
import com.vovan4ok.appliance.store.repository.ClientRepository;
import com.vovan4ok.appliance.store.service.impl.ClientServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    @Mock
    ClientRepository clientRepository;

    @InjectMocks
    ClientServiceImpl clientService;

    @Test
    void findAll_returnsList() {
        List<Client> clients = List.of(
                new Client(1L, "John", "john@mail.com", "hashed", "1234-5678"),
                new Client(2L, "Jane", "jane@mail.com", "hashed", "8765-4321")
        );
        when(clientRepository.findAll()).thenReturn(clients);

        List<Client> result = clientService.findAll();

        assertThat(result).hasSize(2);
        verify(clientRepository).findAll();
    }

    @Test
    void findAll_pageable_returnsPage() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Client> page = new PageImpl<>(List.of(new Client(1L, "John", "john@mail.com", "hashed", "1234-5678")));
        when(clientRepository.findAll(pageable)).thenReturn(page);

        Page<Client> result = clientService.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(clientRepository).findAll(pageable);
    }

    @Test
    void findById_found_returnsClient() {
        Client client = new Client(1L, "John", "john@mail.com", "hashed", "1234-5678");
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        Optional<Client> result = clientService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getCard()).isEqualTo("1234-5678");
    }

    @Test
    void findById_notFound_returnsEmpty() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Client> result = clientService.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void save_delegatesToRepository() {
        Client client = new Client(null, "John", "john@mail.com", "hashed", "1234-5678");
        Client saved = new Client(1L, "John", "john@mail.com", "hashed", "1234-5678");
        when(clientRepository.save(client)).thenReturn(saved);

        Client result = clientService.save(client);

        assertThat(result.getId()).isEqualTo(1L);
        verify(clientRepository).save(client);
    }

    @Test
    void delete_callsDeleteById() {
        clientService.delete(1L);

        verify(clientRepository).deleteById(1L);
    }

    @Test
    void findByEmail_found_returnsClient() {
        Client client = new Client(1L, "John", "john@mail.com", "hashed", "1234-5678");
        when(clientRepository.findByEmail("john@mail.com")).thenReturn(Optional.of(client));

        Optional<Client> result = clientService.findByEmail("john@mail.com");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("John");
    }

    @Test
    void findByEmail_notFound_returnsEmpty() {
        when(clientRepository.findByEmail("nobody@mail.com")).thenReturn(Optional.empty());

        Optional<Client> result = clientService.findByEmail("nobody@mail.com");

        assertThat(result).isEmpty();
    }
}
