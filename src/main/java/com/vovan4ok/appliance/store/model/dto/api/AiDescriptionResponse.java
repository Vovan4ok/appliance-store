package com.vovan4ok.appliance.store.model.dto.api;

public record AiDescriptionResponse(Long applianceId,
                                    String language,
                                    String description,
                                    Source source,
                                    String fallbackReason) {
    public enum Source {
        AI,
        FALLBACK
    }
}
