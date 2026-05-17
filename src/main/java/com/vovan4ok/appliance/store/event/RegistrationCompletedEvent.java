package com.vovan4ok.appliance.store.event;

/**
 * Published after a new client successfully registers.
 */
public record RegistrationCompletedEvent(String name, String email) {
}