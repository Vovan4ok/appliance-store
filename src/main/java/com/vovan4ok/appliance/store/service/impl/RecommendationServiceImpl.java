package com.vovan4ok.appliance.store.service.impl;

import com.vovan4ok.appliance.store.model.Appliance;
import com.vovan4ok.appliance.store.model.Recommendation;
import com.vovan4ok.appliance.store.model.RecommendationReason;
import com.vovan4ok.appliance.store.repository.ApplianceRepository;
import com.vovan4ok.appliance.store.repository.OrdersRepository;
import com.vovan4ok.appliance.store.repository.ProductCount;
import com.vovan4ok.appliance.store.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Scores every product in stock against the viewed one ({@link RecommendationScorer}). The catalog is small
 * (~90 products), so this is two queries and a sort in memory; a big catalog would precompute the
 * bought-together counts instead.
 */
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    // Best score first; the lower id wins a tie, so the order is stable between calls
    private static final Comparator<Recommendation> BEST_FIRST = Comparator
            .comparingDouble(Recommendation::score).reversed()
            .thenComparing(r -> r.appliance().getId());

    private final ApplianceRepository applianceRepository;
    private final OrdersRepository ordersRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Recommendation> recommend(Long applianceId, int limit) {
        int size = Math.max(1, Math.min(limit, MAX_LIMIT));
        List<Appliance> available = applianceRepository.findByStockGreaterThan(0);
        if (applianceId == null) {
            return bestSellers(available, size);
        }

        Appliance viewed = applianceRepository.findById(applianceId)
                .orElseThrow(() -> new IllegalArgumentException("Appliance not found: " + applianceId));
        Map<Long, Long> boughtTogether = ordersRepository.countBoughtTogether(applianceId).stream()
                .collect(Collectors.toMap(ProductCount::applianceId, ProductCount::count));
        long maxBoughtTogether = boughtTogether.values().stream().mapToLong(Long::longValue).max().orElse(0);

        return available.stream()
                .filter(candidate -> !candidate.getId().equals(applianceId))
                .map(candidate -> RecommendationScorer.score(viewed, candidate,
                        boughtTogether.getOrDefault(candidate.getId(), 0L), maxBoughtTogether))
                .sorted(BEST_FIRST)
                .limit(size)
                .toList();
    }

    private List<Recommendation> bestSellers(List<Appliance> available, int size) {
        Map<Long, Appliance> unsold = available.stream()
                .collect(Collectors.toMap(Appliance::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        List<Recommendation> result = new ArrayList<>();
        for (ProductCount sold : ordersRepository.countUnitsSold()) {
            if (result.size() == size) {
                return result;
            }
            Appliance appliance = unsold.remove(sold.applianceId());
            if (appliance != null) {
                result.add(new Recommendation(appliance, sold.count(), List.of(RecommendationReason.BEST_SELLER)));
            }
        }
        // Not enough sales yet (e.g. a new shop): fill up with the newest products, without a reason
        unsold.values().stream()
                .sorted(Comparator.comparing(Appliance::getId).reversed())
                .limit(size - result.size())
                .forEach(appliance -> result.add(new Recommendation(appliance, 0, List.of())));
        return result;
    }
}
