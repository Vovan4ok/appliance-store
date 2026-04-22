package com.vovan4ok.appliance.store.repository;

import com.vovan4ok.appliance.store.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByEmail(String email);
}
