package com.vovan4ok.appliance.store.repository.spec;

import com.vovan4ok.appliance.store.model.Appliance;
import com.vovan4ok.appliance.store.model.Category;
import com.vovan4ok.appliance.store.model.PowerType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ApplianceSpecification {

    private ApplianceSpecification() {}

    public static Specification<Appliance> withFilters(
            String name,
            Category category,
            PowerType powerType,
            Long manufacturerId,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (name != null && !name.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (powerType != null) {
                predicates.add(cb.equal(root.get("powerType"), powerType));
            }
            if (manufacturerId != null) {
                predicates.add(cb.equal(root.get("manufacturer").get("id"), manufacturerId));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}