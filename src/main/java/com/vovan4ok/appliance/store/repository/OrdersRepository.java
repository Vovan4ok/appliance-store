package com.vovan4ok.appliance.store.repository;

import com.vovan4ok.appliance.store.model.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrdersRepository extends JpaRepository<Orders, Long> {
    Optional<Orders> findFirstByClient_EmailAndApprovedFalseOrderByIdDesc(String email);
    Page<Orders> findByClient_Email(String email, Pageable pageable);
    Optional<Orders> findByOrderRowSet_Id(Long rowId);

    // Submitted orders waiting for approval (approved: false = cart, null = pending, true = approved)
    long countByApprovedIsNull();

    // For each other product: in how many approved orders it was bought together with the given one
    @Query("""
            select new com.vovan4ok.appliance.store.repository.ProductCount(other.appliance.id, count(distinct o.id))
            from Orders o join o.orderRowSet viewed join o.orderRowSet other
            where o.approved = true and viewed.appliance.id = :applianceId and other.appliance.id <> :applianceId
            group by other.appliance.id""")
    List<ProductCount> countBoughtTogether(@Param("applianceId") Long applianceId);

    // Units sold per product in approved orders, best sellers first
    @Query("""
            select new com.vovan4ok.appliance.store.repository.ProductCount(r.appliance.id, sum(r.number))
            from Orders o join o.orderRowSet r
            where o.approved = true
            group by r.appliance.id
            order by sum(r.number) desc, r.appliance.id""")
    List<ProductCount> countUnitsSold();
}