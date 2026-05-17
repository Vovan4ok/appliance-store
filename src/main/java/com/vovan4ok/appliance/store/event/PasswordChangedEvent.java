package com.vovan4ok.appliance.store.event;

/**
 * Published after a user changes their account password.
 */
public record PasswordChangedEvent(String name, String email) {
}