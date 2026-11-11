package com.vovan4ok.appliance.store.service

import com.vovan4ok.appliance.store.event.OrderApprovedEvent
import com.vovan4ok.appliance.store.event.OrderExpiredEvent
import com.vovan4ok.appliance.store.event.OrderUnapprovedEvent
import com.vovan4ok.appliance.store.exception.InsufficientStockException
import com.vovan4ok.appliance.store.model.Appliance
import com.vovan4ok.appliance.store.model.Client
import com.vovan4ok.appliance.store.model.OrderRow
import com.vovan4ok.appliance.store.model.Orders
import com.vovan4ok.appliance.store.repository.ApplianceInOrderRepository
import com.vovan4ok.appliance.store.repository.ApplianceRepository
import com.vovan4ok.appliance.store.repository.OrdersRepository
import com.vovan4ok.appliance.store.service.impl.OrderServiceImpl
import org.springframework.context.ApplicationEventPublisher
import spock.lang.Specification
import spock.lang.Subject

/**
 * The order lifecycle of OrderServiceImpl: cart (approved false) → pending (null) → approved (true).
 * Stock is taken on approval and given back on unapproval; an order nobody approved in time goes back to the cart.
 * OrderServiceImplTest (JUnit) covers the simple methods that only delegate to the repositories.
 */
class OrderServiceImplSpec extends Specification {

    static final Client ANN = new Client(name: 'Ann', email: 'ann@example.com')

    OrdersRepository ordersRepository = Mock()
    ApplianceRepository applianceRepository = Mock()
    ApplianceInOrderRepository applianceInOrderRepository = Mock()
    ApplicationEventPublisher eventPublisher = Mock()

    @Subject
    OrderServiceImpl service = new OrderServiceImpl(
            ordersRepository, applianceRepository, applianceInOrderRepository, eventPublisher)

    def "approving #requested of #stock in stock leaves #left and notifies the client"() {
        given: 'a pending order for one product'
        def fridge = appliance(1, 'Fridge', stock)
        def order = pendingOrder(7, row(fridge, requested))
        ordersRepository.findById(7L) >> Optional.of(order)

        when:
        service.approve(7L)

        then: 'the units leave the stock'
        fridge.stock == left
        1 * applianceRepository.save(fridge)

        and: 'the order is approved and the approval email is triggered'
        order.approved
        1 * ordersRepository.save(order)
        1 * eventPublisher.publishEvent({ it instanceof OrderApprovedEvent && it.order().orderId == 7L })

        where:
        stock | requested || left
        10    | 1         || 9
        10    | 10        || 0
        3     | 2         || 1
    }

    def "ordering #requested with #stock in stock is refused and changes nothing"() {
        given:
        def fridge = appliance(1, 'Fridge', stock)
        def order = pendingOrder(7, row(fridge, requested))
        ordersRepository.findById(7L) >> Optional.of(order)

        when:
        service.approve(7L)

        then: 'the employee is told how many units there are'
        def e = thrown(InsufficientStockException)
        e.message.contains("\"Fridge\" has only $available unit(s)")

        and: 'the stock and the order stay as they were, no email'
        fridge.stock == stock
        order.approved == null
        0 * applianceRepository.save(_)
        0 * ordersRepository.save(_)
        0 * eventPublisher._

        where:
        stock | requested || available
        0     | 1         || 0
        2     | 3         || 2
        null  | 1         || 0
    }

    def "an order is approved completely or not at all"() {
        given: 'enough fridges, too few ovens'
        def fridge = appliance(1, 'Fridge', 5)
        def oven = appliance(2, 'Oven', 1)
        def order = pendingOrder(7, row(fridge, 2), row(oven, 2))
        ordersRepository.findById(7L) >> Optional.of(order)

        when:
        service.approve(7L)

        then: 'every row is checked before any stock is touched, whatever the order of the rows'
        thrown(InsufficientStockException)
        fridge.stock == 5
        oven.stock == 1
        0 * applianceRepository.save(_)
    }

    def "approving an approved order again takes no more stock"() {
        given:
        def fridge = appliance(1, 'Fridge', 5)
        def order = pendingOrder(7, row(fridge, 2))
        order.approved = true
        ordersRepository.findById(7L) >> Optional.of(order)

        when:
        service.approve(7L)

        then:
        fridge.stock == 5
        0 * applianceRepository.save(_)
        0 * eventPublisher._
    }

    def "unapproving a #state order gives back #restored units and makes it pending"() {
        given:
        def fridge = appliance(1, 'Fridge', 5)
        def order = pendingOrder(7, row(fridge, 2))
        order.approved = approved
        ordersRepository.findById(7L) >> Optional.of(order)

        when:
        service.unapprove(7L)

        then:
        fridge.stock == 5 + restored
        order.approved == null
        events * eventPublisher.publishEvent(_ as OrderUnapprovedEvent)

        where:
        state      | approved || restored | events
        'approved' | true     || 2        | 1
        'pending'  | null     || 0        | 0
    }

    def "returning #description to the cart does nothing"() {
        given:
        ordersRepository.findById(7L) >> Optional.ofNullable(order)

        when:
        def returned = service.returnToCart(7L)

        then:
        !returned
        0 * ordersRepository.save(_)
        0 * ordersRepository.delete(_)
        0 * eventPublisher._

        where:
        description               | order
        'an approved order'       | orderIn(true)
        'an order still in carts' | orderIn(false)
        'a deleted order'         | null
    }

    def "an expired order becomes the client's cart again when there is no newer cart"() {
        given:
        def order = pendingOrder(7, row(appliance(1, 'Fridge', 5), 2))
        order.client = ANN
        ordersRepository.findById(7L) >> Optional.of(order)
        ordersRepository.findFirstByClient_EmailAndApprovedFalseOrderByIdDesc(ANN.email) >> Optional.empty()

        when:
        def returned = service.returnToCart(7L)

        then:
        returned
        order.approved == false
        1 * ordersRepository.save(order)
        0 * ordersRepository.delete(_)
        1 * eventPublisher.publishEvent({ it instanceof OrderExpiredEvent && it.cartOrderId() == 7L })
    }

    def "an expired order merges into the newer cart: #inCart in the cart + #expired expired = #merged"() {
        given: 'the client started a new cart while the order waited'
        def fridge = appliance(1, 'Fridge', 5)
        def order = pendingOrder(7, row(fridge, expired))
        order.client = ANN
        def cart = new Orders(id: 9L, approved: false, client: ANN)
        if (inCart) {
            cart.orderRowSet << row(fridge, inCart)
        }
        ordersRepository.findById(7L) >> Optional.of(order)
        ordersRepository.findFirstByClient_EmailAndApprovedFalseOrderByIdDesc(ANN.email) >> Optional.of(cart)

        when:
        def returned = service.returnToCart(7L)

        then: 'one row per product, quantities added up, price kept'
        returned
        cart.orderRowSet*.number == [merged]
        cart.orderRowSet*.amount == [100.00]

        and: 'the expired order is gone, the email points to the cart'
        1 * ordersRepository.save(cart)
        1 * ordersRepository.delete(order)
        1 * eventPublisher.publishEvent({ it instanceof OrderExpiredEvent && it.cartOrderId() == 9L })

        where:
        inCart | expired || merged
        0      | 2       || 2
        1      | 2       || 3
    }

    private static Appliance appliance(long id, String name, Integer stock) {
        new Appliance(id: id, name: name, stock: stock, price: 100.00)
    }

    private static OrderRow row(Appliance appliance, long number) {
        new OrderRow(appliance: appliance, number: number, amount: appliance.price)
    }

    private static Orders pendingOrder(long id, OrderRow... rows) {
        def order = new Orders(id: id, approved: null)
        order.orderRowSet.addAll(rows)
        order
    }

    private static Orders orderIn(Boolean approved) {
        def order = pendingOrder(7, row(appliance(1, 'Fridge', 5), 1))
        order.approved = approved
        order
    }
}
