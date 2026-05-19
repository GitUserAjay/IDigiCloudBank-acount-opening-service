package com.idigicloud.accountopening.entity;

import com.idigicloud.accountopening.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email"),
        @UniqueConstraint(columnNames = "mobile_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "mobile_number", nullable = false, length = 15)
    private String mobileNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** CBS Customer ID assigned after CBS registration */
    @Column(name = "cbs_customer_id", length = 20)
    private String cbsCustomerId;

    @Column(name = "branch_code", length = 20)
    private String branchCode;

    @Column(name = "is_enabled")
    private boolean isEnabled = true;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AccountOpeningRequest> accountOpeningRequests;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
