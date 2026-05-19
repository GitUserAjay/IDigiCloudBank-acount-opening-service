package com.idigicloud.accountopening.entity;

import com.idigicloud.accountopening.enums.AccountOpeningStatus;
import com.idigicloud.accountopening.enums.ProductClass;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "account_opening_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountOpeningRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── Step 1: New Account ───
    @Enumerated(EnumType.STRING)
    @Column(name = "product_class")
    private ProductClass productClass;

    @Column(name = "customer_type")
    private String customerType; // INDIVIDUAL, CORPORATE

    @Column(name = "branch_code")
    private String branchCode;

    @Column(name = "bank_code")
    private String bankCode;

    @Column(name = "currency_code")
    private String currencyCode;

    // ─── Step 2: Product Selection ───
    @Column(name = "offer_code")
    private String offerCode;

    @Column(name = "offer_name")
    private String offerName;

    @Column(name = "product_code")
    private String productCode;

    @Column(name = "account_type")
    private String accountType; // Savings, Current

    @Column(name = "product_group")
    private String productGroup;

    @Column(name = "total_fees", precision = 10, scale = 2)
    private BigDecimal totalFees;

    // ─── Step 3: Relationship ───
    @Column(name = "cbs_account_number", length = 20)
    private String cbsAccountNumber;

    @Column(name = "mode_of_operation")
    private String modeOfOperation; // SINGLE, ANYONE_OR_SURVIVOR, JOINTLY

    // ─── Step 5: Basic Details ───
    @Column(name = "account_name")
    private String accountName;

    @Column(name = "debit_card_variant")
    private String debitCardVariant;

    // ─── Step 8: Initial Funding ───
    @Column(name = "initial_funding_amount", precision = 15, scale = 2)
    private BigDecimal initialFundingAmount;

    @Column(name = "funding_mode")
    private String fundingMode; // CASH, CHEQUE, FUND_TRANSFER

    // ─── Status Tracking ───
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountOpeningStatus status;

    @Column(name = "current_step")
    private Integer currentStep; // 1 through 7

    @Column(name = "rejection_reason")
    private String rejectionReason;

    // ─── CBS Reference ───
    @Column(name = "cbs_customer_id")
    private String cbsCustomerId;

    @Column(name = "welcome_kit_reference")
    private String welcomeKitReference;

    // ─── Relationships ───
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "accountOpeningRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AccountDocument> documents;

    @OneToMany(mappedBy = "accountOpeningRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Nominee> nominees;

    @OneToMany(mappedBy = "accountOpeningRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CoApplicant> coApplicants;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
