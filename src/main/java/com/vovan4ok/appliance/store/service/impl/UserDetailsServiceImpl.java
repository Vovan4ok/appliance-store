package com.vovan4ok.appliance.store.service.impl;

import com.vovan4ok.appliance.store.repository.ClientRepository;
import com.vovan4ok.appliance.store.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final EmployeeRepository employeeRepository;
    private final ClientRepository clientRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return employeeRepository.findByEmail(email)
                .map(e -> new User(e.getEmail(), e.getPassword(),
                        List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
                .or(() -> clientRepository.findByEmail(email)
                        .map(c -> new User(c.getEmail(), c.getPassword(),
                                List.of(new SimpleGrantedAuthority("ROLE_CLIENT")))))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
