package com.idigicloud.accountopening.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String cbsCustomerId;
    private String branchCode;

    public static AuthResponse of(String token, com.idigicloud.accountopening.entity.User user) {
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .cbsCustomerId(user.getCbsCustomerId())
                .branchCode(user.getBranchCode())
                .build();
    }
}
