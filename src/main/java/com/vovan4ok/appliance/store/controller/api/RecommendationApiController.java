package com.vovan4ok.appliance.store.controller.api;

import com.vovan4ok.appliance.store.model.RecommendationResult;
import com.vovan4ok.appliance.store.model.dto.api.RecommendationResponse;
import com.vovan4ok.appliance.store.service.RecommendationRateLimiter;
import com.vovan4ok.appliance.store.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * The only public part of the REST API: read-only, called from other websites by the embeddable widget
 * (CORS in {@link com.vovan4ok.appliance.store.config.SecurityConfig}), limited per IP address.
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "Public product recommendations, used by the embeddable widget")
public class RecommendationApiController {

    // Browsers and CDNs may reuse an answer for a few minutes: recommendations change only with new orders
    static final CacheControl CACHE = CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic();
    // While the AI ranking is being prepared the scoring order is sent: ask again soon to get the better one
    static final CacheControl CACHE_WHILE_AI_PENDING = CacheControl.maxAge(Duration.ofSeconds(30)).cachePublic();

    private final RecommendationService recommendationService;
    private final RecommendationRateLimiter rateLimiter;

    @GetMapping
    @Operation(summary = "Recommended products (public, no login)",
            description = "For a product page pass applianceId: similar and bought-together products. "
                    + "Without it: the best sellers. Each item has a score and reason codes.")
    public ResponseEntity<List<RecommendationResponse>> recommend(
            @Parameter(description = "The product being viewed")
            @RequestParam(required = false) Long applianceId,
            @Parameter(description = "How many, 1 to 12")
            @RequestParam(defaultValue = "4") int limit,
            @Parameter(description = "Language of the AI explanations: en (default) or uk")
            @RequestParam(defaultValue = "en") String lang,
            HttpServletRequest request) {
        // The client's address: on Cloud Run the forwarded-headers support puts it here
        rateLimiter.acquire(request.getRemoteAddr());

        // Only two values reach the service (and the AI cache key), whatever the client sends
        String language = lang.toLowerCase(Locale.ROOT).startsWith("uk") ? "uk" : "en";
        RecommendationResult result = recommendationService.recommend(applianceId, limit, language);
        List<RecommendationResponse> body = result.items().stream()
                .map(RecommendationResponse::from)
                .toList();
        return ResponseEntity.ok().cacheControl(result.aiPending() ? CACHE_WHILE_AI_PENDING : CACHE).body(body);
    }
}
