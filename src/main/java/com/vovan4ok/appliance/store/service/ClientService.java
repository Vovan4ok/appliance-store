package com.vovan4ok.appliance.store.service;

import com.vovan4ok.appliance.store.model.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ClientService {

    List<Client> findAll();

    Page<Client> findAll(Pageable pageable);

    Optional<Client> findById(Long id);

    Client save(Client client);

    void delete(Long id);

    Optional<Client> findByEmail(String email);
}
