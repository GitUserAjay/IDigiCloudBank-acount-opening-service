package com.idigicloud.accountopening.service.impl;

import com.idigicloud.accountopening.cbs.CbsClient;
import com.idigicloud.accountopening.cbs.CbsServiceException;
import com.idigicloud.accountopening.exception.InvalidOperationException;
import com.idigicloud.accountopening.dto.request.LoginRequest;
import com.idigicloud.accountopening.dto.request.RegisterRequest;
import com.idigicloud.accountopening.dto.response.AuthResponse;
import com.idigicloud.accountopening.entity.User;
import com.idigicloud.accountopening.enums.Role;
import com.idigicloud.accountopening.exception.DuplicateResourceException;
import com.idigicloud.accountopening.repository.UserRepository;
import com.idigicloud.accountopening.security.JwtUtil;
import com.idigicloud.accountopening.service.AuthService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final CbsClient cbsClient;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }
        if (userRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new DuplicateResourceException("Mobile number already registered: " + request.getMobileNumber());
        }

        // Step 1: Register customer in CBS
        String cbsCustomerId = registerInCbs(request);

        // Step 2: Save user locally
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .mobileNumber(request.getMobileNumber())
                .role(Role.ROLE_CUSTOMER)
                .cbsCustomerId(cbsCustomerId)
                .branchCode(request.getBranchCode() != null ? request.getBranchCode() : "UTIBOO134")
                .isEnabled(true)
                .build();

        User saved = userRepository.save(user);
        log.info("User registered: id={}, cbsCustomerId={}", saved.getId(), cbsCustomerId);

        UserDetails userDetails = userDetailsService.loadUserByUsername(saved.getEmail());
        String token = jwtUtil.generateToken(userDetails);
        return AuthResponse.of(token, saved);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);
        log.info("User logged in: {}", user.getEmail());
        return AuthResponse.of(token, user);
    }

    /**
     * Creates customer in CBS and returns the CBS Customer ID (max 20 chars for users.cbs_customer_id).
     */
    private String registerInCbs(RegisterRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("firstName", request.getFirstName());
        payload.put("lastName", request.getLastName());
        payload.put("email", request.getEmail());
        payload.put("mobileNumber", request.getMobileNumber());
        payload.put("branchCode", request.getBranchCode() != null ? request.getBranchCode() : "UTIBOO134");
        payload.put("bankCode", "AXB");

        try {
            JsonNode response = cbsClient.createCustomer(payload);
            String customerId = extractCustomerId(response);
            if (customerId != null) {
                validateCbsCustomerIdLength(customerId);
                return customerId;
            }
        } catch (CbsServiceException e) {
            log.warn("CBS create customer failed: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("CBS create customer failed: {}", e.getMessage());
        }

        // Customer may already exist in CBS from a previous partial registration
        try {
            JsonNode searchResponse = cbsClient.searchCustomer("EMAIL", request.getEmail());
            String existingId = extractCustomerId(searchResponse);
            if (existingId != null) {
                validateCbsCustomerIdLength(existingId);
                log.info("Reusing existing CBS customer for email={}: {}", request.getEmail(), existingId);
                return existingId;
            }
        } catch (Exception e) {
            log.warn("CBS search by email failed: {}", e.getMessage());
        }

        throw new InvalidOperationException(
                "Could not register customer in CBS. Please retry in a minute or contact support.");
    }

    private String extractCustomerId(JsonNode response) {
        if (response != null && response.has("data") && response.get("data").has("customerId")) {
            String customerId = response.get("data").get("customerId").asText();
            if (customerId != null && !customerId.isBlank()) {
                return customerId;
            }
        }
        return null;
    }

    private void validateCbsCustomerIdLength(String customerId) {
        if (customerId.length() > 20) {
            throw new InvalidOperationException(
                    "CBS customer ID is too long for local storage. Please contact support.");
        }
    }
}
