package com.idigicloud.accountopening.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "co_applicants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoApplicant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cbs_customer_id", nullable = false)
    private String cbsCustomerId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_role") // JOF, JOO, GUARANTOR
    private String customerRole;

    @Column(name = "is_existing_customer")
    private boolean isExistingCustomer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_opening_request_id", nullable = false)
    private AccountOpeningRequest accountOpeningRequest;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
