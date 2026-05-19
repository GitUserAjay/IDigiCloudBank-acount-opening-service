package com.idigicloud.accountopening.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_type", nullable = false) // AADHAAR, PAN, DRIVING_LICENSE, RATION_CARD
    private String documentType;

    @Column(name = "document_category")  // ID_PROOF, ADDRESS_PROOF
    private String documentCategory;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "document_id")  // CBS document ID after upload
    private String documentId;

    @Column(name = "version_no")
    private Integer versionNo;

    @Column(name = "index_category") // CUSTOMER, ACCOUNT
    private String indexCategory;

    @Column(name = "is_verified")
    private boolean isVerified = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_opening_request_id", nullable = false)
    private AccountOpeningRequest accountOpeningRequest;

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;
}
