package com.vovan4ok.appliance.store.service;

import com.vovan4ok.appliance.store.model.dto.api.AiDescriptionResponse;

import java.util.Locale;

public interface AiDescriptionService {
    AiDescriptionResponse generate(Long applianceId, Locale locale);
}
