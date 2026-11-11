package com.vovan4ok.appliance.store.service;

import com.vovan4ok.appliance.store.model.Appliance;
import com.vovan4ok.appliance.store.model.Category;
import com.vovan4ok.appliance.store.model.Recommendation;
import com.vovan4ok.appliance.store.model.RecommendationResult;
import com.vovan4ok.appliance.store.repository.ApplianceRepository;
import com.vovan4ok.appliance.store.repository.OrdersRepository;
import com.vovan4ok.appliance.store.repository.ProductCount;
import com.vovan4ok.appliance.store.service.impl.AiRecommendationRanker;
import com.vovan4ok.appliance.store.service.impl.RecommendationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

import static com.vovan4ok.appliance.store.model.RecommendationReason.BEST_SELLER;
import static com.vovan4ok.appliance.store.model.RecommendationReason.BOUGHT_TOGETHER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

    @Mock
    ApplianceRepository applianceRepository;

    @Mock
    OrdersRepository ordersRepository;

    @Mock
    AiRecommendationRanker aiRanker;

    @InjectMocks
    RecommendationServiceImpl service;

    private final Appliance viewed = appliance(1L, Category.BIG, "1000.00");

    @Test
    void forAProduct_returnsTheBestScoredOthers_withoutTheProductItself() {
        Appliance twin = appliance(2L, Category.BIG, "1000.00");
        Appliance companion = appliance(3L, Category.SMALL, "50.00");
        Appliance unrelated = appliance(4L, Category.SMALL, "20.00");
        when(applianceRepository.findByStockGreaterThan(0)).thenReturn(List.of(viewed, twin, companion, unrelated));
        when(applianceRepository.findById(1L)).thenReturn(Optional.of(viewed));
        when(ordersRepository.countBoughtTogether(1L)).thenReturn(List.of(new ProductCount(3L, 5L)));

        List<Recommendation> result = service.recommend(1L, 4);

        assertThat(result).extracting(r -> r.appliance().getId()).containsExactly(3L, 2L, 4L);
        assertThat(result.get(0).reasons()).contains(BOUGHT_TOGETHER);
    }

    @Test
    void outOfStockProducts_areNeverRecommended() {
        // findByStockGreaterThan(0) is the only source of candidates
        Appliance inStock = appliance(2L, Category.BIG, "1000.00");
        when(applianceRepository.findByStockGreaterThan(0)).thenReturn(List.of(inStock));
        when(applianceRepository.findById(1L)).thenReturn(Optional.of(viewed));
        when(ordersRepository.countBoughtTogether(1L)).thenReturn(List.of(new ProductCount(99L, 3L)));

        assertThat(service.recommend(1L, 4)).extracting(r -> r.appliance().getId()).containsExactly(2L);
    }

    @Test
    void equalScores_areOrderedById_soTheResultIsStable() {
        Appliance a = appliance(7L, Category.SMALL, null);
        Appliance b = appliance(5L, Category.SMALL, null);
        when(applianceRepository.findByStockGreaterThan(0)).thenReturn(List.of(a, b));
        when(applianceRepository.findById(1L)).thenReturn(Optional.of(viewed));
        when(ordersRepository.countBoughtTogether(1L)).thenReturn(List.of());

        assertThat(service.recommend(1L, 4)).extracting(r -> r.appliance().getId()).containsExactly(5L, 7L);
    }

    @Test
    void limit_isClampedToOneUpToTheMaximum() {
        List<Appliance> many = LongStream.rangeClosed(2, 30)
                .mapToObj(id -> appliance(id, Category.BIG, "1000.00")).toList();
        when(applianceRepository.findByStockGreaterThan(0)).thenReturn(many);
        when(applianceRepository.findById(1L)).thenReturn(Optional.of(viewed));
        when(ordersRepository.countBoughtTogether(1L)).thenReturn(List.of());

        assertThat(service.recommend(1L, 100)).hasSize(RecommendationService.MAX_LIMIT);
        assertThat(service.recommend(1L, 0)).hasSize(1);
        assertThat(service.recommend(1L, -5)).hasSize(1);
    }

    @Test
    void unknownProduct_isRejected() {
        when(applianceRepository.findByStockGreaterThan(0)).thenReturn(List.of());
        when(applianceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recommend(99L, 4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    @Test
    void withoutAProduct_returnsBestSellersInStock_mostSoldFirst() {
        Appliance top = appliance(2L, Category.BIG, "1.00");
        Appliance second = appliance(3L, Category.BIG, "1.00");
        when(applianceRepository.findByStockGreaterThan(0)).thenReturn(List.of(second, top));
        // 9 is sold out: skipped even though it sold the most
        when(ordersRepository.countUnitsSold()).thenReturn(List.of(
                new ProductCount(9L, 10L), new ProductCount(2L, 4L), new ProductCount(3L, 1L)));

        List<Recommendation> result = service.recommend(null, 2);

        assertThat(result).extracting(r -> r.appliance().getId()).containsExactly(2L, 3L);
        assertThat(result).extracting(Recommendation::score).containsExactly(4.0, 1.0);
        assertThat(result).allSatisfy(r -> assertThat(r.reasons()).containsExactly(BEST_SELLER));
    }

    @Test
    void withFewSales_bestSellersAreFilledUpWithTheNewestProducts() {
        when(applianceRepository.findByStockGreaterThan(0)).thenReturn(List.of(
                appliance(2L, Category.BIG, "1.00"), appliance(3L, Category.BIG, "1.00"),
                appliance(4L, Category.BIG, "1.00"), appliance(5L, Category.BIG, "1.00")));
        when(ordersRepository.countUnitsSold()).thenReturn(List.of(new ProductCount(3L, 2L)));

        List<Recommendation> result = service.recommend(null, 3);

        assertThat(result).extracting(r -> r.appliance().getId()).containsExactly(3L, 5L, 4L);
        assertThat(result.get(1).reasons()).isEmpty();
    }

    @Test
    void aiRanking_getsAllCandidatesInScoringOrder_andTheLimitIsAppliedAfterIt() {
        List<Appliance> many = LongStream.rangeClosed(2, 7)
                .mapToObj(id -> appliance(id, Category.BIG, "1000.00")).toList();
        when(applianceRepository.findByStockGreaterThan(0)).thenReturn(many);
        when(applianceRepository.findById(1L)).thenReturn(Optional.of(viewed));
        when(ordersRepository.countBoughtTogether(1L)).thenReturn(List.of());
        // The AI puts the last candidate first and explains it
        when(aiRanker.rank(eq(viewed), anyList(), eq("uk"))).thenAnswer(call -> {
            List<Recommendation> scored = call.getArgument(1);
            List<Recommendation> reordered = new java.util.ArrayList<>(scored);
            reordered.add(0, reordered.remove(scored.size() - 1).withExplanation("Пасує до перегляданого"));
            return new AiRecommendationRanker.Ranked(reordered, false);
        });

        RecommendationResult result = service.recommend(1L, 2, "uk");

        verify(aiRanker).rank(eq(viewed), argThat(list -> list.size() == 6), eq("uk"));
        assertThat(result.items()).extracting(r -> r.appliance().getId()).containsExactly(7L, 2L);
        assertThat(result.items().get(0).explanation()).isEqualTo("Пасує до перегляданого");
        assertThat(result.aiPending()).isFalse();
    }

    @Test
    void aiPending_isPassedOn() {
        when(applianceRepository.findByStockGreaterThan(0)).thenReturn(List.of(appliance(2L, Category.BIG, "1.00")));
        when(applianceRepository.findById(1L)).thenReturn(Optional.of(viewed));
        when(ordersRepository.countBoughtTogether(1L)).thenReturn(List.of());
        when(aiRanker.rank(eq(viewed), anyList(), eq("en")))
                .thenAnswer(call -> new AiRecommendationRanker.Ranked(call.getArgument(1), true));

        assertThat(service.recommend(1L, 4, "en").aiPending()).isTrue();
    }

    @Test
    void bestSellers_areNeverSentToTheAi() {
        when(applianceRepository.findByStockGreaterThan(0)).thenReturn(List.of(appliance(2L, Category.BIG, "1.00")));
        when(ordersRepository.countUnitsSold()).thenReturn(List.of(new ProductCount(2L, 3L)));

        RecommendationResult result = service.recommend(null, 4, "en");

        assertThat(result.items()).hasSize(1);
        assertThat(result.aiPending()).isFalse();
        verifyNoInteractions(aiRanker);
    }

    private static Appliance appliance(Long id, Category category, String price) {
        Appliance a = new Appliance();
        a.setId(id);
        a.setName("Appliance " + id);
        a.setCategory(category);
        a.setPrice(price != null ? new BigDecimal(price) : null);
        a.setStock(5);
        return a;
    }
}
