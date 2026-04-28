package com.vovan4ok.appliance.store.controller;

import com.vovan4ok.appliance.store.exception.InsufficientStockException;
import com.vovan4ok.appliance.store.model.Category;
import com.vovan4ok.appliance.store.model.Appliance;
import com.vovan4ok.appliance.store.model.Orders;
import com.vovan4ok.appliance.store.model.PowerType;
import com.vovan4ok.appliance.store.model.dto.ApplianceDto;
import com.vovan4ok.appliance.store.model.dto.ClientViewDto;
import com.vovan4ok.appliance.store.model.dto.EmployeeViewDto;
import com.vovan4ok.appliance.store.model.dto.ManufacturerDto;
import com.vovan4ok.appliance.store.model.dto.OrderDto;
import com.vovan4ok.appliance.store.model.dto.OrderViewDto;
import com.vovan4ok.appliance.store.service.ApplianceService;
import com.vovan4ok.appliance.store.service.ClientService;
import com.vovan4ok.appliance.store.service.EmployeeService;
import com.vovan4ok.appliance.store.service.ManufacturerService;
import com.vovan4ok.appliance.store.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Slf4j
@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrdersController {

    private final OrderService orderService;
    private final ClientService clientService;
    private final EmployeeService employeeService;
    private final ApplianceService applianceService;
    private final ManufacturerService manufacturerService;

    @GetMapping
    public String list(Model model,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "5") int size,
                       Authentication auth) {
        log.debug("GET /orders page={} size={}", page, size);
        boolean isEmployee = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));
        Page<Orders> result = isEmployee
                ? orderService.findAll(PageRequest.of(page, size, Sort.by("id")))
                : orderService.findByClientEmail(auth.getName(), PageRequest.of(page, size, Sort.by("id")));
        model.addAttribute("orders", result.getContent().stream().map(OrderViewDto::from).toList());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("isEmployee", isEmployee);
        return "order/orders";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        log.debug("GET /orders/add");
        model.addAttribute("order", new OrderDto());
        model.addAttribute("clients", clientService.findAll().stream().map(ClientViewDto::from).toList());
        model.addAttribute("employees", employeeService.findAll().stream().map(EmployeeViewDto::from).toList());
        return "order/newOrder";
    }

    @PostMapping("/add-order")
    public String save(@ModelAttribute("order") OrderDto dto) {
        Orders order = new Orders();
        order.setClient(clientService.findById(dto.getClientId()).orElse(null));
        order.setEmployee(employeeService.findById(dto.getEmployeeId()).orElse(null));
        order.setApproved(false);
        Orders saved = orderService.save(order);
        log.info("Order created id={} client={}", saved.getId(), saved.getClient());
        return "redirect:/orders/" + saved.getId() + "/edit";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        log.debug("GET /orders/{}/edit", id);
        Orders order = orderService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        OrderViewDto orderDto = OrderViewDto.from(order);
        model.addAttribute("order", orderDto);
        model.addAttribute("rows", orderDto.getRows());
        return "order/editOrder";
    }

    @GetMapping("/{id}/choice-appliance")
    public String choiceAppliance(@PathVariable Long id,
                                  Model model,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "6") int size,
                                  @RequestParam(required = false) String name,
                                  @RequestParam(required = false) Category category,
                                  @RequestParam(required = false) PowerType powerType,
                                  @RequestParam(required = false) Long manufacturerId,
                                  @RequestParam(required = false) BigDecimal minPrice,
                                  @RequestParam(required = false) BigDecimal maxPrice,
                                  @RequestParam(defaultValue = "name") String sortBy,
                                  @RequestParam(defaultValue = "asc") String sortDir,
                                  @RequestParam(defaultValue = "false") boolean inStockOnly) {
        log.debug("GET /orders/{}/choice-appliance page={} size={}", id, page, size);

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Page<Appliance> result = applianceService.findAll(
                name, category, powerType, manufacturerId, minPrice, maxPrice,
                inStockOnly, false, PageRequest.of(page, size, sort));

        model.addAttribute("ordersId", id);
        model.addAttribute("appliances", result.getContent().stream().map(ApplianceDto::from).toList());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", result.getTotalPages());

        model.addAttribute("filterName", name);
        model.addAttribute("filterCategory", category);
        model.addAttribute("filterPowerType", powerType);
        model.addAttribute("filterManufacturerId", manufacturerId);
        model.addAttribute("filterMinPrice", minPrice);
        model.addAttribute("filterMaxPrice", maxPrice);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("filterInStockOnly", inStockOnly);

        model.addAttribute("categories", Category.values());
        model.addAttribute("powerTypes", PowerType.values());
        model.addAttribute("manufacturers", manufacturerService.findAll().stream().map(ManufacturerDto::from).toList());

        return "order/choiceAppliance";
    }

    @PostMapping("/{id}/add-into-order")
    public String addIntoOrder(@PathVariable Long id,
                               @RequestParam Long applianceId,
                               @RequestParam Long numbers,
                               @RequestParam BigDecimal price) {
        log.info("Adding appliance id={} x{} to order id={}", applianceId, numbers, id);
        orderService.addOrderRow(id, applianceId, numbers, price);
        return "redirect:/orders/" + id + "/edit";
    }

    @PostMapping("/{id}/rows/{rowId}/update")
    public String updateRow(@PathVariable Long id, @PathVariable Long rowId,
                            @RequestParam Long number) {
        log.info("Updating row id={} quantity={} in order id={}", rowId, number, id);
        orderService.updateOrderRowQuantity(rowId, number);
        return "redirect:/orders/" + id + "/edit";
    }

    @GetMapping("/{id}/rows/{rowId}/remove")
    public String removeRow(@PathVariable Long id, @PathVariable Long rowId) {
        log.info("Removing row id={} from order id={}", rowId, id);
        orderService.removeOrderRow(rowId);
        if (orderService.findById(id).isEmpty()) {
            return "redirect:/orders";
        }
        return "redirect:/orders/" + id + "/edit";
    }

    @GetMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        log.info("Deleting order id={}", id);
        orderService.delete(id);
        return "redirect:/orders";
    }

    @GetMapping("/{id}/approved")
    public String approve(@PathVariable Long id, Model model) {
        log.info("Approving order id={}", id);
        try {
            orderService.approve(id);
            return "redirect:/orders";
        } catch (InsufficientStockException e) {
            log.warn("Approval blocked for order id={}: {}", id, e.getMessage());
            Orders order = orderService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
            OrderViewDto orderDto = OrderViewDto.from(order);
            model.addAttribute("order", orderDto);
            model.addAttribute("rows", orderDto.getRows());
            model.addAttribute("approvalError", true);
            model.addAttribute("currentUri", "/orders/" + id + "/edit");
            return "order/editOrder";
        }
    }

    @GetMapping("/{id}/unapproved")
    public String unapprove(@PathVariable Long id) {
        log.info("Unapproving order id={}", id);
        orderService.unapprove(id);
        return "redirect:/orders";
    }
}
