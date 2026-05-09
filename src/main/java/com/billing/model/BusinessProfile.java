package com.billing.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "business_profile")
public class BusinessProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Business identity ────────────────────────────────────
    @Column(nullable = false, length = 150)
    private String businessName;

    @Column(nullable = false, length = 60)
    private String businessType;     // e.g. RETAIL, RESTAURANT, PHARMACY …

    @Column(length = 100)
    private String ownerName;

    // ── Contact ──────────────────────────────────────────────
    @Column(length = 15)
    private String phone;

    @Column(length = 120)
    private String email;

    @Column(length = 200)
    private String website;

    // ── Address ──────────────────────────────────────────────
    @Column(length = 250)
    private String addressLine1;

    @Column(length = 150)
    private String city;

    @Column(length = 80)
    private String state;

    @Column(length = 10)
    private String pincode;

    // ── Tax / Invoice ────────────────────────────────────────
    @Column(length = 20)
    private String gstin;

    @Column(length = 10)
    private String invoicePrefix = "INV";

    @Column(length = 5)
    private String currencySymbol = "₹";

    // ── Metadata ─────────────────────────────────────────────
    private LocalDateTime registeredAt;

    // ── Getters & Setters ─────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public String getInvoicePrefix() { return invoicePrefix; }
    public void setInvoicePrefix(String invoicePrefix) { this.invoicePrefix = invoicePrefix; }

    public String getCurrencySymbol() { return currencySymbol; }
    public void setCurrencySymbol(String currencySymbol) { this.currencySymbol = currencySymbol; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }

    /** Full formatted address for invoices */
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (addressLine1 != null && !addressLine1.isBlank()) sb.append(addressLine1);
        if (city != null && !city.isBlank()) sb.append(", ").append(city);
        if (state != null && !state.isBlank()) sb.append(", ").append(state);
        if (pincode != null && !pincode.isBlank()) sb.append(" - ").append(pincode);
        return sb.toString();
    }
}
