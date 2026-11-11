package com.vovan4ok.appliance.store.service.impl;

import com.vovan4ok.appliance.store.model.Category;
import com.vovan4ok.appliance.store.model.PowerType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Helpers for putting catalog data into LLM prompts and for reading text back out of them. Catalog fields are
 * typed in by employees, and model output is generated text: both are treated as untrusted.
 */
final class PromptText {

    static final int MAX_FIELD_LENGTH = 300;

    private PromptText() {
    }

    /**
     * Strips control characters and angle brackets (so data can't close a {@code <product>} block or start a
     * new one), drops placeholder values like "-----" and caps the length.
     */
    static String clean(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String cleaned = value.replaceAll("[\\p{Cntrl}<>]", " ").strip();
        if (cleaned.matches("[-\\s]*")) {
            return "";
        }
        return truncate(cleaned, maxLength);
    }

    static String clean(String value) {
        return clean(value, MAX_FIELD_LENGTH);
    }

    static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    /** Cache keys include a hash of the prompt: a changed product or prompt never serves a stale answer. */
    static String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available in the JDK", e);
        }
    }

    // Readable words instead of enum constants, so the model doesn't copy "SMALL" or "AC220" into its text
    static String describe(Category category) {
        if (category == null) {
            return "";
        }
        return switch (category) {
            case BIG -> "large appliance";
            case SMALL -> "small appliance";
        };
    }

    static String describe(PowerType powerType) {
        if (powerType == null) {
            return "";
        }
        return switch (powerType) {
            case AC220 -> "220 V mains";
            case AC110 -> "110 V mains";
            case ACCUMULATOR -> "rechargeable battery";
        };
    }
}
