package com.billing.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "gst_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal gstPercent = BigDecimal.ZERO;

    @Column(name = "gst_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal gstAmount = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;

    public OrderItem() {}

    public OrderItem(Long productId, String productName, int quantity,
                     BigDecimal unitPrice, BigDecimal gstPercent) {
        this.productId   = productId;
        this.productName = productName;
        this.quantity    = quantity;
        this.unitPrice   = unitPrice;
        this.gstPercent  = gstPercent;
        recalculate();
    }

    public void recalculate() {
        BigDecimal base = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.gstAmount = base.multiply(gstPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        this.lineTotal = base.add(gstAmount);
    }

    public BigDecimal getBaseAmount() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getGstPercent() { return gstPercent; }
    public void setGstPercent(BigDecimal gstPercent) { this.gstPercent = gstPercent; }
    public BigDecimal getGstAmount() { return gstAmount; }
    public void setGstAmount(BigDecimal gstAmount) { this.gstAmount = gstAmount; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
}
