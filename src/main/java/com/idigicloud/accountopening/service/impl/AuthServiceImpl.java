package com.idigicloud.accountopening.service.impl;

import com.idigicloud.accountopening.cbs.CbsClient;
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
                .branchCode(request.getBranchCode())
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
     * Creates customer in CBS and returns the CBS Customer ID.
     * Falls back to a timestamped dev ID if CBS is unavailable — never returns null.
     */
    private String registerInCbs(RegisterRequest request) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("firstName", request.getFirstName());
            payload.put("lastName", request.getLastName());
            payload.put("email", request.getEmail());
            payload.put("mobileNumber", request.getMobileNumber());
            payload.put("branchCode", request.getBranchCode() != null ? request.getBranchCode() : "UTIBOO134");
            payload.put("bankCode", "AXB");

            JsonNode response = cbsClient.createCustomer(payload);
            if (response.has("data") && response.get("data").has("customerId")) {
                return response.get("data").get("customerId").asText();
            }
            // CBS returned success but no customerId — use a fallback dev ID
            log.warn("CBS returned no customerId in response, using fallback dev ID");
            return "CBS-DEV-" + System.currentTimeMillis();
        } catch (Exception e) {
            log.warn("CBS registration failed, using fallback dev ID: {}", e.getMessage());
            return "CBS-DEV-" + System.currentTimeMillis();
        }
    }
}
