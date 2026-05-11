package com.billing.controller;

import com.billing.model.BusinessProfile;
import com.billing.model.User;
import com.billing.repository.BusinessProfileRepository;
import com.billing.repository.UserRepository;
import com.billing.service.BillingService;
import com.billing.service.PdfService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final BillingService billingService;
    private final BusinessProfileRepository businessProfileRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PdfService pdfService;

    public ApiController(BillingService billingService,
                         BusinessProfileRepository businessProfileRepository,
                         UserRepository userRepository,
                         PasswordEncoder passwordEncoder,
                         PdfService pdfService) {
        this.billingService = billingService;
        this.businessProfileRepository = businessProfileRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.pdfService = pdfService;
    }

    @Value("${app.version:1.0.0}")
    private String appVersion;
    @Value("${gst.rates:0,5,12,18,28}")
    private String gstRates;
    // Fallback values when no BusinessProfile row exists
    @Value("${app.company:BillingSystem Pro}")
    private String fallbackCompany;
    @Value("${app.address:}")
    private String fallbackAddress;
    @Value("${app.phone:}")
    private String fallbackPhone;
    @Value("${app.gstin:}")
    private String fallbackGstin;

    // ── App Info ────────────────────────────────────────────────
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo(@AuthenticationPrincipal UserDetails user) {
        Optional<BusinessProfile> bpOpt = businessProfileRepository.findAll().stream().findFirst();

        String company = bpOpt.map(BusinessProfile::getBusinessName).orElse(fallbackCompany);
        String address = bpOpt.map(BusinessProfile::getFullAddress).orElse(fallbackAddress);
        String phone   = bpOpt.map(BusinessProfile::getPhone).orElse(fallbackPhone);
        String gstin   = bpOpt.map(BusinessProfile::getGstin).orElse(fallbackGstin);
        String prefix  = bpOpt.map(BusinessProfile::getInvoicePrefix).orElse("INV");

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("company",    company);
        info.put("version",    appVersion);
        info.put("address",    address);
        info.put("phone",      phone);
        info.put("gstin",      gstin);
        info.put("prefix",     prefix);
        info.put("gstRates",   gstRates);
        info.put("username",   user.getUsername());
        info.put("isAdmin",    user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        // Full profile for the UI profile page
        bpOpt.ifPresent(bp -> {
            info.put("businessType", bp.getBusinessType());
            info.put("ownerName",    bp.getOwnerName());
            info.put("email",        bp.getEmail());
            info.put("website",      bp.getWebsite() != null ? bp.getWebsite() : "");
            info.put("city",         bp.getCity() != null ? bp.getCity() : "");
            info.put("state",        bp.getState() != null ? bp.getState() : "");
            info.put("pincode",      bp.getPincode() != null ? bp.getPincode() : "");
        });
        return ResponseEntity.ok(info);
    }

    // ── Dashboard ───────────────────────────────────────────────
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        try {
            return ResponseEntity.ok(billingService.getDashboardData());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage(), "cause", e.getClass().getSimpleName()));
        }
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

    @GetMapping(value = "/bills/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getBillPdf(@PathVariable Long id) {
        var order = billingService.getOrderById(id);
        byte[] pdfBytes = pdfService.generateInvoicePdf(order);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", "Invoice_" + order.getInvoiceNumber() + ".pdf");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
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

    // ── Profile / Change Password ────────────────────────────────
    /**
     * POST /api/profile/password
     * Body: { "currentPassword": "...", "newPassword": "..." }
     */
    @PostMapping("/profile/password")
    public ResponseEntity<?> changePassword(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        String current = body.get("currentPassword");
        String newPwd  = body.get("newPassword");

        if (current == null || current.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Current password is required."));
        if (newPwd == null || newPwd.length() < 6)
            return ResponseEntity.badRequest().body(Map.of("error", "New password must be at least 6 characters."));

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (!passwordEncoder.matches(current, user.getPassword()))
            return ResponseEntity.badRequest().body(Map.of("error", "Current password is incorrect."));

        user.setPassword(passwordEncoder.encode(newPwd));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("success", true, "message", "Password changed successfully!"));
    }

    // ── Error helper ────────────────────────────────────────────
    @ExceptionHandler({IllegalArgumentException.class, java.util.NoSuchElementException.class})
    public ResponseEntity<?> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    private Map<String, Object> orderToDetailMap(com.billing.model.Order o) {
        var items = o.getItems().stream().map(i -> {
            Map<String, Object> im = new LinkedHashMap<>();
            im.put("productId",   i.getProductId());
            im.put("productName", i.getProductName());
            im.put("quantity",    i.getQuantity());
            im.put("unitPrice",   i.getUnitPrice());
            im.put("gstPercent",  i.getGstPercent());
            im.put("gstAmount",   i.getGstAmount());
            im.put("lineTotal",   i.getLineTotal());
            return im;
        }).toList();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",              o.getId());
        m.put("invoiceNumber",   o.getInvoiceNumber());
        m.put("customerName",    o.getCustomer() != null ? o.getCustomer().getName() : "");
        m.put("customerPhone",   o.getCustomer() != null && o.getCustomer().getPhone() != null ? o.getCustomer().getPhone() : "");
        m.put("customerAddress", o.getCustomer() != null && o.getCustomer().getAddress() != null ? o.getCustomer().getAddress() : "");
        m.put("customerGstin",   o.getCustomer() != null && o.getCustomer().getGstin() != null ? o.getCustomer().getGstin() : "");
        m.put("createdBy",       o.getUser() != null ? o.getUser().getFullName() : "");
        m.put("subtotal",        o.getSubtotal());
        m.put("totalGst",        o.getTotalGst());
        m.put("discountAmount",  o.getDiscountAmount());
        m.put("totalAmount",     o.getTotalAmount());
        m.put("paymentMethod",   o.getPaymentMethod());
        m.put("paymentStatus",   o.getPaymentStatus());
        m.put("notes",           o.getNotes() != null ? o.getNotes() : "");
        m.put("createdAt",       o.getCreatedAt());
        m.put("items",           items);
        return m;
    }
}
