package com.vovan4ok.appliance.store.service.impl;

import com.vovan4ok.appliance.store.aspect.Loggable;
import com.vovan4ok.appliance.store.model.Client;
import com.vovan4ok.appliance.store.repository.ClientRepository;
import com.vovan4ok.appliance.store.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;

    @Loggable
    @Override
    public List<Client> findAll() {
        return clientRepository.findAll();
    }

    @Override
    public Page<Client> findAll(Pageable pageable) {
        return clientRepository.findAll(pageable);
    }

    @Override
    public Optional<Client> findById(Long id) {
        return clientRepository.findById(id);
    }

    @Loggable
    @Transactional
    @Override
    public Client save(Client client) {
        return clientRepository.save(client);
    }

    @Loggable
    @Transactional
    @Override
    public void delete(Long id) {
        clientRepository.deleteById(id);
    }

    @Override
    public Optional<Client> findByEmail(String email) {
        return clientRepository.findByEmail(email);
    }

    @Transactional
    @Override
    public void updateAvatar(Long id, String avatarPath) {
        clientRepository.findById(id).ifPresent(c -> {
            c.setAvatarPath(avatarPath);
            clientRepository.save(c);
        });
    }
}
