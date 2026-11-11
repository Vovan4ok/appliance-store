package com.vovan4ok.appliance.store.repository;

import com.vovan4ok.appliance.store.model.Category;
import com.vovan4ok.appliance.store.model.report.OrderPipeline;
import com.vovan4ok.appliance.store.model.report.RevenueShare;
import com.vovan4ok.appliance.store.model.report.StageTotals;
import com.vovan4ok.appliance.store.model.report.TopProduct;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record6;
import org.jooq.Records;
import org.jooq.SelectConditionStep;
import org.jooq.SelectField;
import org.jooq.SelectSelectStep;
import org.jooq.Table;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

import static com.vovan4ok.appliance.store.jooq.Tables.APPLIANCE;
import static com.vovan4ok.appliance.store.jooq.Tables.MANUFACTURER;
import static com.vovan4ok.appliance.store.jooq.Tables.ORDERS;
import static com.vovan4ok.appliance.store.jooq.Tables.ORDER_ROW;
import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.countDistinct;
import static org.jooq.impl.DSL.partitionBy;
import static org.jooq.impl.DSL.rank;
import static org.jooq.impl.DSL.round;
import static org.jooq.impl.DSL.row;
import static org.jooq.impl.DSL.sum;

/**
 * Read-only sales figures, written in jOOQ instead of JPA. Every query here aggregates over many rows and returns
 * a few numbers, which is where typed SQL is the better tool:
 * <ul>
 *   <li>window functions (a group's share of the total, rank within a category) and filtered aggregates
 *       (three pipeline stages in one pass) are written as SQL, checked by the compiler against the tables
 *       generated from the Flyway migrations, and rendered for H2 (dev/test) and PostgreSQL (prod);</li>
 *   <li>results map straight into records ({@link Records#mapping}: a wrong column count or type doesn't
 *       compile), no entities are loaded into the persistence context;</li>
 *   <li>queries are composed from Java values: {@link #revenueBy} is one query for any grouping column.</li>
 * </ul>
 * Writes stay with JPA (entities, cascades, validation); both share the DataSource and Spring transactions.
 */
@Repository
@RequiredArgsConstructor
public class SalesReportRepository {

    /** Value of one order line: unit price × quantity. */
    private static final Field<BigDecimal> LINE_VALUE = ORDER_ROW.AMOUNT.mul(ORDER_ROW.NUMBER);
    private static final Field<Long> UNITS = sum(ORDER_ROW.NUMBER).cast(SQLDataType.BIGINT);
    private static final Field<BigDecimal> REVENUE = sum(LINE_VALUE);

    private final DSLContext dsl;

    /**
     * Carts, pending and approved orders with their units and value, in one pass over the orders: every column
     * is an aggregate with a FILTER (WHERE ...) clause, emulated with CASE where a database lacks FILTER.
     */
    public OrderPipeline orderPipeline() {
        return dsl.select(
                        stage(ORDERS.APPROVED.isFalse()),
                        stage(ORDERS.APPROVED.isNull()),
                        stage(ORDERS.APPROVED.isTrue()))
                .from(ORDERS)
                .leftJoin(ORDER_ROW).on(ORDER_ROW.ORDER_ID.eq(ORDERS.ID))
                .fetchSingle(Records.mapping(OrderPipeline::new));
    }

    /** Approved revenue per category, largest first, with each category's share of the total. */
    public List<RevenueShare<Category>> revenueByCategory() {
        return revenueBy(APPLIANCE.CATEGORY);
    }

    /** Approved revenue per brand, largest first, with each brand's share of the total. */
    public List<RevenueShare<String>> revenueByBrand() {
        return revenueBy(MANUFACTURER.NAME);
    }

    /**
     * The best sellers of every category (by units, then revenue): rank() over a partition per category, computed
     * in a derived table and filtered outside it, because a window function can't be used in WHERE.
     */
    public List<TopProduct> topProductsPerCategory(int perCategory) {
        Field<Integer> salesRank = rank()
                .over(partitionBy(APPLIANCE.CATEGORY).orderBy(UNITS.desc(), REVENUE.desc()))
                .as("sales_rank");
        Field<Long> units = UNITS.as("units");
        Field<BigDecimal> revenue = REVENUE.as("revenue");

        Table<Record6<Category, Integer, Long, String, Long, BigDecimal>> ranked = approvedSales(
                dsl.select(APPLIANCE.CATEGORY, salesRank, APPLIANCE.ID, APPLIANCE.NAME, units, revenue))
                .groupBy(APPLIANCE.CATEGORY, APPLIANCE.ID, APPLIANCE.NAME)
                .asTable("ranked");

        Field<Category> category = ranked.field(APPLIANCE.CATEGORY);
        Field<Integer> position = ranked.field(salesRank);
        Field<Long> applianceId = ranked.field(APPLIANCE.ID);
        return dsl.select(category, position, applianceId, ranked.field(APPLIANCE.NAME),
                        ranked.field(units), ranked.field(revenue))
                .from(ranked)
                .where(position.le(perCategory))
                .orderBy(category, position, applianceId)
                .fetch(Records.mapping(TopProduct::new));
    }

    /** One query for any grouping column; the column's type becomes the key type of the result. */
    private <K> List<RevenueShare<K>> revenueBy(Field<K> key) {
        // The cast fixes the scale: H2 would return the division with ~150 decimal places, PostgreSQL with 1
        Field<BigDecimal> share = round(REVENUE.mul(100).div(sum(REVENUE).over()), 1)
                .cast(SQLDataType.NUMERIC(5, 1));
        return approvedSales(dsl.select(key, countDistinct(ORDERS.ID), UNITS, REVENUE, share))
                .groupBy(key)
                .orderBy(REVENUE.desc(), key.asc().nullsLast())
                .fetch(Records.mapping(RevenueShare::new));
    }

    /** Rows of approved orders with their product and brand (a product without a brand still counts). */
    private static <R extends Record> SelectConditionStep<R> approvedSales(SelectSelectStep<R> select) {
        return select.from(ORDERS)
                .join(ORDER_ROW).on(ORDER_ROW.ORDER_ID.eq(ORDERS.ID))
                .join(APPLIANCE).on(APPLIANCE.ID.eq(ORDER_ROW.APPLIANCE_ID))
                .leftJoin(MANUFACTURER).on(MANUFACTURER.ID.eq(APPLIANCE.MANUFACTURER_ID))
                .where(ORDERS.APPROVED.isTrue());
    }

    /** Orders, units and value of the orders matching {@code inStage}, as one nested record. */
    private static SelectField<StageTotals> stage(Condition inStage) {
        return row(
                countDistinct(ORDERS.ID).filterWhere(inStage),
                coalesce(sum(ORDER_ROW.NUMBER).filterWhere(inStage), BigDecimal.ZERO).cast(SQLDataType.BIGINT),
                coalesce(sum(LINE_VALUE).filterWhere(inStage), BigDecimal.ZERO))
                .mapping(StageTotals::new);
    }
}
