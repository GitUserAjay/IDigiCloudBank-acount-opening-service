package com.idigicloud.accountopening.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "nominees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Nominee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "gender", nullable = false)
    private String gender;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "relationship", nullable = false)
    private String relationship;

    @Column(name = "share_percentage", nullable = false)
    private Integer sharePercentage;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "country")
    private String country;

    @Column(name = "postal_code")
    private String postalCode;

    /** CBS nominee reference ID (returned after CBS sync) */
    @Column(name = "cbs_nominee_id")
    private Long cbsNomineeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_opening_request_id", nullable = false)
    private AccountOpeningRequest accountOpeningRequest;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
