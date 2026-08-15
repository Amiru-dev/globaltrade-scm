package com.globaltrade.common.entity;

import com.globaltrade.common.enums.ComplianceStatus;
import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a supplier/manufacturer in the global supply chain network.
 * Tracks vendor performance ratings, compliance status, and contract details.
 *
 * <p>Vendor performance is periodically evaluated by the {@code VendorPerformanceTimer}
 * which calculates ratings based on on-time delivery, quality, and responsiveness.</p>
 */
@Entity
@Table(name = "vendors", indexes = {
    @Index(name = "idx_vendor_country", columnList = "country"),
    @Index(name = "idx_vendor_compliance", columnList = "compliance_status"),
    @Index(name = "idx_vendor_rating", columnList = "performance_rating")
})
@NamedQueries({
    @NamedQuery(
        name = "Vendor.findAll",
        query = "SELECT v FROM Vendor v ORDER BY v.performanceRating DESC"
    ),
    @NamedQuery(
        name = "Vendor.findByComplianceStatus",
        query = "SELECT v FROM Vendor v WHERE v.complianceStatus = :status"
    ),
    @NamedQuery(
        name = "Vendor.findExpiredContracts",
        query = "SELECT v FROM Vendor v WHERE v.contractExpiry < :date"
    ),
    @NamedQuery(
        name = "Vendor.findByCountry",
        query = "SELECT v FROM Vendor v WHERE v.country = :country"
    ),
    @NamedQuery(
        name = "Vendor.findLowPerformance",
        query = "SELECT v FROM Vendor v WHERE v.performanceRating < :threshold ORDER BY v.performanceRating"
    )
})
public class Vendor implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vendor_id")
    private Long vendorId;

    @Column(name = "vendor_code", unique = true, nullable = false, length = 20)
    private String vendorCode;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "performance_rating", precision = 3, scale = 2)
    private BigDecimal performanceRating;

    @Enumerated(EnumType.STRING)
    @Column(name = "compliance_status", nullable = false, length = 30)
    private ComplianceStatus complianceStatus;

    @Column(name = "contract_expiry")
    private LocalDate contractExpiry;

    @Column(name = "annual_volume", precision = 15, scale = 2)
    private BigDecimal annualVolume;

    @Column(name = "payment_terms", length = 50)
    private String paymentTerms;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.complianceStatus == null) {
            this.complianceStatus = ComplianceStatus.PENDING;
        }
        if (this.performanceRating == null) {
            this.performanceRating = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ======================== Constructors ========================

    public Vendor() {
    }

    public Vendor(String vendorCode, String name, String country, String category) {
        this.vendorCode = vendorCode;
        this.name = name;
        this.country = country;
        this.category = category;
        this.complianceStatus = ComplianceStatus.PENDING;
        this.performanceRating = BigDecimal.ZERO;
    }

    // ======================== Getters and Setters ========================

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String vendorCode) { this.vendorCode = vendorCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getPerformanceRating() { return performanceRating; }
    public void setPerformanceRating(BigDecimal performanceRating) { this.performanceRating = performanceRating; }

    public ComplianceStatus getComplianceStatus() { return complianceStatus; }
    public void setComplianceStatus(ComplianceStatus complianceStatus) { this.complianceStatus = complianceStatus; }

    public LocalDate getContractExpiry() { return contractExpiry; }
    public void setContractExpiry(LocalDate contractExpiry) { this.contractExpiry = contractExpiry; }

    public BigDecimal getAnnualVolume() { return annualVolume; }
    public void setAnnualVolume(BigDecimal annualVolume) { this.annualVolume = annualVolume; }

    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public String toString() {
        return "Vendor{" +
                "vendorId=" + vendorId +
                ", vendorCode='" + vendorCode + '\'' +
                ", name='" + name + '\'' +
                ", country='" + country + '\'' +
                ", rating=" + performanceRating +
                ", compliance=" + complianceStatus +
                '}';
    }
}
