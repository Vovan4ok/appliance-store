package com.vovan4ok.appliance.store.service;

import com.vovan4ok.appliance.store.model.Client;
import com.vovan4ok.appliance.store.model.Employee;
import com.vovan4ok.appliance.store.repository.ClientRepository;
import com.vovan4ok.appliance.store.repository.EmployeeRepository;
import com.vovan4ok.appliance.store.service.impl.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    EmployeeRepository employeeRepository;
    @Mock
    ClientRepository clientRepository;

    @InjectMocks
    UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsername_foundEmployee_returnsEmployeeRole() {
        Employee employee = new Employee(1L, "Alice", "alice@mail.com", "hashed", "sales");
        when(employeeRepository.findByEmail("alice@mail.com")).thenReturn(Optional.of(employee));

        UserDetails result = userDetailsService.loadUserByUsername("alice@mail.com");

        assertThat(result.getUsername()).isEqualTo("alice@mail.com");
        assertThat(result.getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));
    }

    @Test
    void loadUserByUsername_foundClient_returnsClientRole() {
        Client client = new Client(2L, "Bob", "bob@mail.com", "hashed", "1234-5678");
        when(employeeRepository.findByEmail("bob@mail.com")).thenReturn(Optional.empty());
        when(clientRepository.findByEmail("bob@mail.com")).thenReturn(Optional.of(client));

        UserDetails result = userDetailsService.loadUserByUsername("bob@mail.com");

        assertThat(result.getUsername()).isEqualTo("bob@mail.com");
        assertThat(result.getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));
    }

    @Test
    void loadUserByUsername_notFound_throwsUsernameNotFoundException() {
        when(employeeRepository.findByEmail("unknown@mail.com")).thenReturn(Optional.empty());
        when(clientRepository.findByEmail("unknown@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown@mail.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("unknown@mail.com");
    }
}
