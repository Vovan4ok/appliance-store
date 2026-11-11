package com.vovan4ok.appliance.store.security;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Accounts that may look at everything but change nothing ({@code app.demo.read-only-accounts}): the sample
 * logins of the public demo, whose passwords are in the README. Empty by default, so dev and tests are unaffected.
 */
public final class ReadOnlyDemoAccounts {

    private final Set<String> emails;

    public ReadOnlyDemoAccounts(Collection<String> emails) {
        this.emails = emails.stream()
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .map(email -> email.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isReadOnly(String email) {
        return email != null && emails.contains(email.trim().toLowerCase(Locale.ROOT));
    }
}
