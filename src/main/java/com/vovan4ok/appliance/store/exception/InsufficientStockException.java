package com.vovan4ok.appliance.store.exception;

import lombok.Getter;

@Getter
public class InsufficientStockException extends RuntimeException {

    private final String applianceName;
    private final int available;

    public InsufficientStockException(String applianceName, int available) {
        super("Cannot approve: \"" + applianceName + "\" has only " + available + " unit(s) in stock.");
        this.applianceName = applianceName;
        this.available = available;
    }
}