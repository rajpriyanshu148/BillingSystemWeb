package com.billing.service;

import com.billing.model.Customer;
import com.billing.model.Order;
import com.billing.model.OrderItem;
import com.billing.model.Product;
import com.billing.repository.CustomerRepository;
import com.billing.repository.OrderRepository;
import com.billing.repository.ProductRepository;
import com.billing.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class BillingService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public BillingService(OrderRepository orderRepository,
                          ProductRepository productRepository,
                          CustomerRepository customerRepository,
                          UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    @Value("${invoice.prefix:INV}")
    private String invoicePrefix;

    // ── Dashboard ──────────────────────────────────────────────

    public Map<String, Object> getDashboardData() {
        java.time.LocalDateTime startOfDay = java.time.LocalDate.now().atStartOfDay();
        java.time.LocalDateTime startOfNextDay = startOfDay.plusDays(1);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("todayRevenue", orderRepository.getTodayRevenue(startOfDay, startOfNextDay));
        data.put("todayBillCount", orderRepository.getTodayBillCount(startOfDay, startOfNextDay));
        data.put("productCount", productRepository.countActive());
        data.put("customerCount", customerRepository.countAll());
        data.put("recentOrders", orderRepository.findTop10ByOrderByCreatedAtDesc()
                .stream().map(this::orderToMap).toList());
        return data;
    }

    // ── Products ───────────────────────────────────────────────

    public List<Product> getActiveProducts() {
        return productRepository.findByActiveTrueOrderByNameAsc();
    }

    public List<Product> getAllProducts() {
        return productRepository.findAllByOrderByNameAsc();
    }

    public List<Product> searchProducts(String keyword) {
        return productRepository.findByActiveTrueAndNameContainingIgnoreCase(keyword);
    }

    public Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + id));
    }

    @Transactional
    public Product saveProduct(Product product) {
        validateProduct(product);
        return productRepository.save(product);
    }

    @Transactional
    public boolean deleteProduct(Long id) {
        return productRepository.softDelete(id) > 0;
    }

    // ── Customers ──────────────────────────────────────────────

    public List<Customer> getAllCustomers() {
        return customerRepository.findAllByOrderByNameAsc();
    }

    public Customer getCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + id));
    }

    @Transactional
    public Customer saveCustomer(Customer customer) {
        validateCustomer(customer);
        return customerRepository.save(customer);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        customerRepository.deleteById(id);
    }

    // ── Orders / Billing ───────────────────────────────────────

    @Transactional
    public Order saveBill(BillRequest req, String username) {
        if (req.getItems() == null || req.getItems().isEmpty())
            throw new IllegalArgumentException("Bill must have at least one item.");

        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        var customer = customerRepository.findById(req.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        Order order = new Order();
        order.setUser(user);
        order.setCustomer(customer);
        order.setPaymentMethod(Order.PaymentMethod.valueOf(req.getPaymentMethod()));
        order.setNotes(req.getNotes());

        for (BillRequest.ItemLine line : req.getItems()) {
            Product p = productRepository.findById(line.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + line.getProductId()));
            if (p.getQuantity() < line.getQuantity())
                throw new IllegalArgumentException("Insufficient stock for \"" + p.getName() +
                        "\". Available: " + p.getQuantity() + ", Requested: " + line.getQuantity());

            OrderItem item = new OrderItem(p.getId(), p.getName(), line.getQuantity(),
                    p.getPrice(), p.getGstPercent());
            item.setOrder(order);
            order.getItems().add(item);

            int deducted = productRepository.decreaseStock(p.getId(), line.getQuantity());
            if (deducted == 0)
                throw new IllegalArgumentException("Insufficient stock for \"" + p.getName() + "\"");
        }

        BigDecimal discount = req.getDiscountAmount() != null ? req.getDiscountAmount() : BigDecimal.ZERO;
        order.recalculate(discount);

        if (order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Bill total must be greater than zero.");

        order.setInvoiceNumber(generateInvoiceNumber());
        return orderRepository.save(order);
    }

    public Optional<Order> getOrderByInvoice(String invoiceNo) {
        return orderRepository.findByInvoiceNumber(invoiceNo);
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + id));
    }

    // ── Reports ────────────────────────────────────────────────

    public List<Map<String, Object>> getDailyReport() {
        return orderRepository.getDailyReport().stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", row[0]);
            m.put("billCount", row[1]);
            m.put("revenue", row[2]);
            m.put("gst", row[3]);
            return m;
        }).toList();
    }

    public List<Map<String, Object>> getMonthlyReport() {
        return orderRepository.getMonthlyReport().stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("year", row[0]);
            m.put("month", row[1]);
            m.put("billCount", row[2]);
            m.put("revenue", row[3]);
            m.put("gst", row[4]);
            return m;
        }).toList();
    }

    // ── Private helpers ────────────────────────────────────────

    private String generateInvoiceNumber() {
        String datePart = LocalDate.now().toString().replace("-", "");
        long count = orderRepository.count() + 1;
        return invoicePrefix + "-" + datePart + "-" + String.format("%04d", count);
    }

    private void validateProduct(Product p) {
        if (p.getName() == null || p.getName().trim().length() < 2)
            throw new IllegalArgumentException("Product name must be at least 2 characters.");
        if (p.getPrice() == null || p.getPrice().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Price must be 0 or greater.");
        if (p.getQuantity() < 0)
            throw new IllegalArgumentException("Quantity cannot be negative.");
    }

    private void validateCustomer(Customer c) {
        if (c.getName() == null || c.getName().trim().isEmpty())
            throw new IllegalArgumentException("Customer name is required.");
        if (c.getPhone() != null && !c.getPhone().isEmpty()
                && !c.getPhone().matches("[6-9]\\d{9}"))
            throw new IllegalArgumentException("Enter a valid 10-digit Indian mobile number.");
    }

    private Map<String, Object> orderToMap(Order o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", o.getId());
        m.put("invoiceNumber", o.getInvoiceNumber());
        m.put("customerName", o.getCustomer() != null ? o.getCustomer().getName() : "");
        m.put("totalAmount", o.getTotalAmount());
        m.put("paymentMethod", o.getPaymentMethod());
        m.put("paymentStatus", o.getPaymentStatus());
        m.put("createdAt", o.getCreatedAt());
        return m;
    }

    // ── Request DTOs ───────────────────────────────────────────

    public static class BillRequest {
        private Long customerId;
        private String paymentMethod = "CASH";
        private BigDecimal discountAmount = BigDecimal.ZERO;
        private String notes;
        private List<ItemLine> items;

        public static class ItemLine {
            private Long productId;
            private int quantity;
            public Long getProductId() { return productId; }
            public void setProductId(Long productId) { this.productId = productId; }
            public int getQuantity() { return quantity; }
            public void setQuantity(int quantity) { this.quantity = quantity; }
        }

        public Long getCustomerId() { return customerId; }
        public void setCustomerId(Long customerId) { this.customerId = customerId; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public BigDecimal getDiscountAmount() { return discountAmount; }
        public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public List<ItemLine> getItems() { return items; }
        public void setItems(List<ItemLine> items) { this.items = items; }
    }
}
