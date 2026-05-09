package com.billing.controller;

import com.billing.service.BillingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final BillingService billingService;

    public ApiController(BillingService billingService) {
        this.billingService = billingService;
    }

    @Value("${app.company:BillingSystem Pro}")
    private String appCompany;
    @Value("${app.version:1.0.0}")
    private String appVersion;
    @Value("${app.address:123 Main Street, Your City}")
    private String appAddress;
    @Value("${app.phone:+91-XXXXXXXXXX}")
    private String appPhone;
    @Value("${app.gstin:GSTIN_NUMBER}")
    private String appGstin;
    @Value("${gst.rates:0,5,12,18,28}")
    private String gstRates;

    // ── App Info ────────────────────────────────────────────────
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo(@AuthenticationPrincipal UserDetails user) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("company", appCompany);
        info.put("version", appVersion);
        info.put("address", appAddress);
        info.put("phone", appPhone);
        info.put("gstin", appGstin);
        info.put("gstRates", gstRates);
        info.put("username", user.getUsername());
        info.put("isAdmin", user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        return ResponseEntity.ok(info);
    }

    // ── Dashboard ───────────────────────────────────────────────
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        return ResponseEntity.ok(billingService.getDashboardData());
    }

    // ── Products ────────────────────────────────────────────────
    @GetMapping("/products")
    public ResponseEntity<?> getProducts(@RequestParam(required = false) String search) {
        if (search != null && !search.isBlank())
            return ResponseEntity.ok(billingService.searchProducts(search));
        return ResponseEntity.ok(billingService.getActiveProducts());
    }

    @GetMapping("/products/all")
    public ResponseEntity<?> getAllProducts() {
        return ResponseEntity.ok(billingService.getAllProducts());
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(billingService.getProduct(id));
    }

    @PostMapping("/products")
    public ResponseEntity<?> createProduct(@RequestBody com.billing.model.Product product) {
        product.setId(null);
        return ResponseEntity.ok(billingService.saveProduct(product));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody com.billing.model.Product product) {
        product.setId(id);
        return ResponseEntity.ok(billingService.saveProduct(product));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        billingService.deleteProduct(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ── Customers ───────────────────────────────────────────────
    @GetMapping("/customers")
    public ResponseEntity<?> getCustomers() {
        return ResponseEntity.ok(billingService.getAllCustomers());
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<?> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(billingService.getCustomer(id));
    }

    @PostMapping("/customers")
    public ResponseEntity<?> createCustomer(@RequestBody com.billing.model.Customer customer) {
        customer.setId(null);
        return ResponseEntity.ok(billingService.saveCustomer(customer));
    }

    @PutMapping("/customers/{id}")
    public ResponseEntity<?> updateCustomer(@PathVariable Long id, @RequestBody com.billing.model.Customer customer) {
        customer.setId(id);
        return ResponseEntity.ok(billingService.saveCustomer(customer));
    }

    @DeleteMapping("/customers/{id}")
    public ResponseEntity<?> deleteCustomer(@PathVariable Long id) {
        billingService.deleteCustomer(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ── Billing ─────────────────────────────────────────────────
    @PostMapping("/bills")
    public ResponseEntity<?> createBill(@RequestBody BillingService.BillRequest req,
                                         @AuthenticationPrincipal UserDetails user) {
        var order = billingService.saveBill(req, user.getUsername());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "invoiceNumber", order.getInvoiceNumber(),
                "totalAmount", order.getTotalAmount(),
                "orderId", order.getId()
        ));
    }

    @GetMapping("/bills/{id}")
    public ResponseEntity<?> getBill(@PathVariable Long id) {
        var order = billingService.getOrderById(id);
        return ResponseEntity.ok(orderToDetailMap(order));
    }

    @GetMapping("/bills/invoice/{invoiceNo}")
    public ResponseEntity<?> getBillByInvoice(@PathVariable String invoiceNo) {
        return billingService.getOrderByInvoice(invoiceNo)
                .map(o -> ResponseEntity.ok(orderToDetailMap(o)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Reports ─────────────────────────────────────────────────
    @GetMapping("/reports/daily")
    public ResponseEntity<?> getDailyReport() {
        return ResponseEntity.ok(billingService.getDailyReport());
    }

    @GetMapping("/reports/monthly")
    public ResponseEntity<?> getMonthlyReport() {
        return ResponseEntity.ok(billingService.getMonthlyReport());
    }

    // ── Error helper ────────────────────────────────────────────
    @ExceptionHandler({IllegalArgumentException.class, java.util.NoSuchElementException.class})
    public ResponseEntity<?> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    private Map<String, Object> orderToDetailMap(com.billing.model.Order o) {
        var items = o.getItems().stream().map(i -> {
            Map<String, Object> im = new LinkedHashMap<>();
            im.put("productId", i.getProductId());
            im.put("productName", i.getProductName());
            im.put("quantity", i.getQuantity());
            im.put("unitPrice", i.getUnitPrice());
            im.put("gstPercent", i.getGstPercent());
            im.put("gstAmount", i.getGstAmount());
            im.put("lineTotal", i.getLineTotal());
            return im;
        }).toList();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", o.getId());
        m.put("invoiceNumber", o.getInvoiceNumber());
        m.put("customerName", o.getCustomer() != null ? o.getCustomer().getName() : "");
        m.put("customerPhone", o.getCustomer() != null && o.getCustomer().getPhone() != null ? o.getCustomer().getPhone() : "");
        m.put("customerAddress", o.getCustomer() != null && o.getCustomer().getAddress() != null ? o.getCustomer().getAddress() : "");
        m.put("customerGstin", o.getCustomer() != null && o.getCustomer().getGstin() != null ? o.getCustomer().getGstin() : "");
        m.put("createdBy", o.getUser() != null ? o.getUser().getFullName() : "");
        m.put("subtotal", o.getSubtotal());
        m.put("totalGst", o.getTotalGst());
        m.put("discountAmount", o.getDiscountAmount());
        m.put("totalAmount", o.getTotalAmount());
        m.put("paymentMethod", o.getPaymentMethod());
        m.put("paymentStatus", o.getPaymentStatus());
        m.put("notes", o.getNotes() != null ? o.getNotes() : "");
        m.put("createdAt", o.getCreatedAt());
        m.put("items", items);
        return m;
    }
}
